package org.koikifw.buildsupport.referencee2e;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Test-owned durable receipt ledger in an independent JDBC transaction. */
final class LocalNotificationProviderStub implements AutoCloseable {
    private final PostgreSQLContainer postgres;
    private final HttpServer server;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean rejectBeforeReceipt = new AtomicBoolean();
    private final AtomicBoolean loseAfterReceipt = new AtomicBoolean();
    private final AtomicBoolean queryUnavailable = new AtomicBoolean();
    private final AtomicReference<Exception> observationFailure = new AtomicReference<>();
    private final AtomicReference<Instant> verificationTime = new AtomicReference<>(Instant.now());

    LocalNotificationProviderStub(PostgreSQLContainer postgres) throws Exception {
        this.postgres = postgres;
        try (var c = admin(); var s = c.createStatement()) {
            s.execute("""
                    CREATE SCHEMA kkref_r1_provider_stub;
                    CREATE TABLE kkref_r1_provider_stub.accepted_notifications (
                      notification_key TEXT PRIMARY KEY, canonical_payload_hash TEXT NOT NULL,
                      acceptance_id UUID NOT NULL UNIQUE, event_id UUID NOT NULL,
                      recipient_id UUID,
                      accepted_at TIMESTAMPTZ NOT NULL DEFAULT now());
                    CREATE ROLE kkref_r1_stub_writer NOLOGIN;
                    CREATE TABLE kkref_r1_provider_stub.arrived_requests (
                      request_id UUID PRIMARY KEY, notification_key TEXT, event_id TEXT,
                      payload_hash TEXT, response_status INT,
                      arrived_at TIMESTAMPTZ NOT NULL DEFAULT now());
                    CREATE TABLE kkref_r1_provider_stub.key_contracts (
                      notification_key TEXT PRIMARY KEY, payload_hash TEXT NOT NULL,
                      recipient_id UUID NOT NULL, valid_until TIMESTAMPTZ NOT NULL);
                    GRANT USAGE ON SCHEMA kkref_r1_provider_stub TO kkref_r1_stub_writer;
                    GRANT SELECT, INSERT ON kkref_r1_provider_stub.accepted_notifications TO kkref_r1_stub_writer;
                    DO $$ BEGIN IF NOT EXISTS(SELECT 1 FROM pg_roles WHERE rolname='kkref_r1_observer')
                      THEN CREATE ROLE kkref_r1_observer NOLOGIN; END IF; END $$;
                    GRANT pg_read_all_stats TO kkref_r1_observer;
                    GRANT USAGE ON SCHEMA kkref_r1_provider_stub TO kkref_r1_observer;
                    GRANT SELECT ON kkref_r1_provider_stub.accepted_notifications TO kkref_r1_observer;
                    GRANT SELECT ON kkref_r1_provider_stub.arrived_requests TO kkref_r1_observer;
                    GRANT SELECT ON kkref_r1_provider_stub.key_contracts TO kkref_r1_observer;
                    """);
        }
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/notifications", this::receive);
        server.createContext("/notifications/receipt", this::inspect);
        server.setExecutor(executor);
        server.start();
    }

    URI endpoint() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/notifications");
    }

    Connection admin() throws Exception {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    Connection observer() throws Exception {
        if (observationFailure.get() != null) {
            throw new IllegalStateException("Provider observation failed", observationFailure.get());
        }
        Connection c = admin();
        try {
            try (var s = c.createStatement()) { s.execute("SET ROLE kkref_r1_observer"); }
            return c;
        } catch (Exception failure) {
            c.close();
            throw failure;
        }
    }

    void grantApplicationObservation(boolean publications) throws Exception {
        try (var c = admin(); var s = c.createStatement()) {
            s.execute("GRANT SELECT ON kkref_expense_request, kkref_department, koiki_audit_event, kkref_flyway_history TO kkref_r1_observer");
            if (publications) {
                s.execute("GRANT USAGE ON SCHEMA kkref_notification_async TO kkref_r1_observer");
                s.execute("GRANT SELECT ON kkref_notification_async.event_publication TO kkref_r1_observer");
                s.execute("GRANT SELECT ON kkref_notification_async.notification_receipt_observation TO kkref_r1_observer");
            }
        }
    }

    void rejectBeforeReceipt() { rejectBeforeReceipt.set(true); }

    void loseAfterReceipt() { loseAfterReceipt.set(true); }

    void queryUnavailable() { queryUnavailable.set(true); }

    Instant verificationTime() { return verificationTime.get(); }

    void setVerificationTime(Instant time) { verificationTime.set(java.util.Objects.requireNonNull(time)); }

    void registerContract(String key, String hash, UUID recipient, Instant validUntil) throws Exception {
        try (var c = admin(); var s = c.prepareStatement("""
                INSERT INTO kkref_r1_provider_stub.key_contracts
                  (notification_key, payload_hash, recipient_id, valid_until) VALUES (?, ?, ?, ?)
                ON CONFLICT(notification_key) DO UPDATE SET payload_hash=EXCLUDED.payload_hash,
                  recipient_id=EXCLUDED.recipient_id, valid_until=EXCLUDED.valid_until
                """)) {
            s.setString(1, key); s.setString(2, hash); s.setObject(3, recipient);
            s.setObject(4, java.sql.Timestamp.from(validUntil));
            s.executeUpdate();
        }
    }

    void removeContract(String key) throws Exception {
        try (var c = admin(); var s = c.prepareStatement(
                "DELETE FROM kkref_r1_provider_stub.key_contracts WHERE notification_key=?")) {
            s.setString(1, key);
            s.executeUpdate();
        }
    }

    long arrivalCount() throws Exception {
        try (var c = observer(); var s = c.createStatement();
                var rows = s.executeQuery("SELECT count(*) FROM kkref_r1_provider_stub.arrived_requests")) {
            rows.next(); return rows.getLong(1);
        }
    }

    long rejectedCount(String key) throws Exception {
        try (var c = observer(); var s = c.prepareStatement("""
                SELECT count(*) FROM kkref_r1_provider_stub.arrived_requests
                WHERE notification_key=? AND response_status=503
                """)) {
            s.setString(1, key);
            try (var rows = s.executeQuery()) { rows.next(); return rows.getLong(1); }
        }
    }

    long receiptCount() throws Exception {
        try (var c = observer(); var s = c.createStatement();
                var rows = s.executeQuery("SELECT count(*) FROM kkref_r1_provider_stub.accepted_notifications")) {
            rows.next();
            return rows.getLong(1);
        }
    }

    String receipt(String key) throws Exception {
        try (var c = observer(); var s = c.prepareStatement(
                "SELECT acceptance_id FROM kkref_r1_provider_stub.accepted_notifications WHERE notification_key=?")) {
            s.setString(1, key);
            try (var rows = s.executeQuery()) {
                if (!rows.next()) { throw new AssertionError("Expected provider receipt absent"); }
                return rows.getString(1);
            }
        }
    }

    static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private void receive(HttpExchange exchange) {
        try (exchange) {
            String key = exchange.getRequestHeaders().getFirst("Idempotency-Key");
            String hash = exchange.getRequestHeaders().getFirst("X-Payload-SHA256");
            String event = exchange.getRequestHeaders().getFirst("X-Koiki-Event-Id");
            String recipient = exchange.getRequestHeaders().getFirst("X-Recipient-Id");
            UUID requestId = UUID.randomUUID();
            exchange.setAttribute("arrivalId", requestId);
            try (var c = admin(); var s = c.prepareStatement("""
                    INSERT INTO kkref_r1_provider_stub.arrived_requests
                      (request_id,notification_key,event_id,payload_hash) VALUES (?,?,?,?)
                    """)) {
                s.setObject(1, requestId); s.setString(2, key);
                s.setString(3, event); s.setString(4, hash); s.executeUpdate();
            }
            byte[] bytes = exchange.getRequestBody().readNBytes(4097);
            if (!"POST".equals(exchange.getRequestMethod()) || key == null || key.length() > 200
                    || hash == null || event == null || bytes.length > 4096
                    || !sha256(new String(bytes, StandardCharsets.UTF_8)).equals(hash)) {
                reply(exchange, 400, "invalid request");
                return;
            }
            UUID eventId = UUID.fromString(event);
            if (rejectBeforeReceipt.get()) {
                reply(exchange, 503, "R2 provider rejected before receipt");
                return;
            }
            try (var c = admin(); var contract = c.prepareStatement("""
                    SELECT payload_hash, recipient_id, valid_until
                    FROM kkref_r1_provider_stub.key_contracts WHERE notification_key=?
                    """)) {
                contract.setString(1, key);
                try (var row = contract.executeQuery()) {
                    if (row.next()) {
                        if (!hash.equals(row.getString(1)) || !recipient.equals(row.getString(2))) {
                            reply(exchange, 409, "contract mismatch");
                            return;
                        }
                        if (!row.getTimestamp(3).toInstant().isAfter(verificationTime())) {
                            reply(exchange, 410, "expired");
                            return;
                        }
                    }
                }
            }
            try (var c = admin()) {
                c.setAutoCommit(false);
                try (var role = c.createStatement()) { role.execute("SET LOCAL ROLE kkref_r1_stub_writer"); }
                int inserted;
                try (var insert = c.prepareStatement("""
                        INSERT INTO kkref_r1_provider_stub.accepted_notifications
                          (notification_key,canonical_payload_hash,acceptance_id,event_id,recipient_id)
                        VALUES (?,?,?,?,?) ON CONFLICT(notification_key) DO NOTHING
                        """)) {
                    insert.setString(1, key);
                    insert.setString(2, hash);
                    insert.setObject(3, UUID.randomUUID());
                    insert.setObject(4, eventId);
                    insert.setObject(5, recipient == null ? null : UUID.fromString(recipient));
                    inserted = insert.executeUpdate();
                }
                try (var select = c.prepareStatement("""
                        SELECT canonical_payload_hash,acceptance_id,recipient_id
                        FROM kkref_r1_provider_stub.accepted_notifications WHERE notification_key=?
                        """)) {
                    select.setString(1, key);
                    try (var rows = select.executeQuery()) {
                        if (!rows.next()) { throw new IllegalStateException("Receipt not visible"); }
                        if (!hash.equals(rows.getString(1))
                                || (recipient != null && !recipient.equals(rows.getString(3)))) {
                            c.rollback();
                            reply(exchange, 409, "payload conflict");
                            return;
                        }
                        String receipt = rows.getString(2);
                        c.commit();
                        if (loseAfterReceipt.get()) {
                            exchange.close();
                            return;
                        }
                        reply(exchange, inserted == 1 ? 201 : 200, receipt);
                    }
                }
            }
        } catch (Exception exception) {
            observationFailure.compareAndSet(null, exception);
            // Do not emit request payload, SQL, credentials, or token material.
            exchange.close();
        }
    }

    private void inspect(HttpExchange exchange) {
        try (exchange) {
            if (queryUnavailable.get()) {
                replyInspection(exchange, 503, "unavailable");
                return;
            }
            String key = exchange.getRequestHeaders().getFirst("Idempotency-Key");
            String hash = exchange.getRequestHeaders().getFirst("X-Payload-SHA256");
            String recipient = exchange.getRequestHeaders().getFirst("X-Recipient-Id");
            String event = exchange.getRequestHeaders().getFirst("X-Koiki-Event-Id");
            if (!"POST".equals(exchange.getRequestMethod()) || key == null || hash == null
                    || recipient == null || event == null) {
                replyInspection(exchange, 409, "contract mismatch");
                return;
            }
            try (var c = admin(); var contract = c.prepareStatement("""
                    SELECT payload_hash, recipient_id, valid_until
                    FROM kkref_r1_provider_stub.key_contracts WHERE notification_key=?
                    """); var accepted = c.prepareStatement("""
                    SELECT canonical_payload_hash, event_id, acceptance_id, recipient_id
                    FROM kkref_r1_provider_stub.accepted_notifications WHERE notification_key=?
                    """)) {
                contract.setString(1, key);
                try (var rows = contract.executeQuery()) {
                    if (!rows.next() || !hash.equals(rows.getString(1))
                            || !recipient.equals(rows.getString(2))) {
                        replyInspection(exchange, 409, "contract mismatch");
                        return;
                    }
                    Instant validUntil = rows.getTimestamp(3).toInstant();
                    accepted.setString(1, key);
                    try (var receipt = accepted.executeQuery()) {
                        if (receipt.next()) {
                            if (!hash.equals(receipt.getString(1)) || !event.equals(receipt.getString(2))
                                    || !recipient.equals(receipt.getString(4))) {
                                replyInspection(exchange, 409, "receipt mismatch");
                                return;
                            }
                            replyInspection(exchange, 200, receipt.getString(3));
                            return;
                        }
                    }
                    if (!validUntil.isAfter(verificationTime())) {
                        replyInspection(exchange, 410, "expired");
                    } else {
                        replyInspection(exchange, 404, validUntil.toString());
                    }
                }
            }
        } catch (Exception exception) {
            observationFailure.compareAndSet(null, exception);
            exchange.close();
        }
    }

    private static void replyInspection(HttpExchange exchange, int status, String value) throws Exception {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private void reply(HttpExchange exchange, int status, String text) throws Exception {
        try (var c = admin(); var s = c.prepareStatement("""
                UPDATE kkref_r1_provider_stub.arrived_requests SET response_status=? WHERE request_id=?
                """)) {
            s.setInt(1, status); s.setObject(2, exchange.getAttribute("arrivalId")); s.executeUpdate();
        }
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @Override
    public void close() throws Exception {
        server.stop(0);
        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            executor.shutdownNow();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Provider fixture did not terminate");
            }
        }
    }
}
