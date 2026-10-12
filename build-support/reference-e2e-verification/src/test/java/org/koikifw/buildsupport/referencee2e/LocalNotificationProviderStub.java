package org.koikifw.buildsupport.referencee2e;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Test-owned durable receipt ledger in an independent JDBC transaction. */
final class LocalNotificationProviderStub implements AutoCloseable {
    private final PostgreSQLContainer postgres;
    private final HttpServer server;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    LocalNotificationProviderStub(PostgreSQLContainer postgres) throws Exception {
        this.postgres = postgres;
        try (var c = admin(); var s = c.createStatement()) {
            s.execute("""
                    CREATE SCHEMA kkref_r1_provider_stub;
                    CREATE TABLE kkref_r1_provider_stub.accepted_notifications (
                      notification_key TEXT PRIMARY KEY, canonical_payload_hash TEXT NOT NULL,
                      acceptance_id UUID NOT NULL UNIQUE, event_id UUID NOT NULL,
                      accepted_at TIMESTAMPTZ NOT NULL DEFAULT now());
                    CREATE ROLE kkref_r1_stub_writer NOLOGIN;
                    GRANT USAGE ON SCHEMA kkref_r1_provider_stub TO kkref_r1_stub_writer;
                    GRANT SELECT, INSERT ON kkref_r1_provider_stub.accepted_notifications TO kkref_r1_stub_writer;
                    DO $$ BEGIN IF NOT EXISTS(SELECT 1 FROM pg_roles WHERE rolname='kkref_r1_observer')
                      THEN CREATE ROLE kkref_r1_observer NOLOGIN; END IF; END $$;
                    GRANT pg_read_all_stats TO kkref_r1_observer;
                    GRANT USAGE ON SCHEMA kkref_r1_provider_stub TO kkref_r1_observer;
                    GRANT SELECT ON kkref_r1_provider_stub.accepted_notifications TO kkref_r1_observer;
                    """);
        }
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/notifications", this::receive);
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
        Connection c = admin();
        try (var s = c.createStatement()) {
            s.execute("SET ROLE kkref_r1_observer");
        }
        return c;
    }

    void grantApplicationObservation(boolean publications) throws Exception {
        try (var c = admin(); var s = c.createStatement()) {
            s.execute("GRANT SELECT ON kkref_expense_request, koiki_audit_event, kkref_flyway_history TO kkref_r1_observer");
            if (publications) {
                s.execute("GRANT USAGE ON SCHEMA kkref_notification_async TO kkref_r1_observer");
                s.execute("GRANT SELECT ON kkref_notification_async.event_publication TO kkref_r1_observer");
            }
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
            byte[] bytes = exchange.getRequestBody().readNBytes(4097);
            if (!"POST".equals(exchange.getRequestMethod()) || key == null || key.length() > 200
                    || hash == null || event == null || bytes.length > 4096
                    || !sha256(new String(bytes, StandardCharsets.UTF_8)).equals(hash)) {
                reply(exchange, 400, "invalid request");
                return;
            }
            UUID eventId = UUID.fromString(event);
            try (var c = admin()) {
                c.setAutoCommit(false);
                try (var role = c.createStatement()) { role.execute("SET LOCAL ROLE kkref_r1_stub_writer"); }
                int inserted;
                try (var insert = c.prepareStatement("""
                        INSERT INTO kkref_r1_provider_stub.accepted_notifications
                          (notification_key,canonical_payload_hash,acceptance_id,event_id)
                        VALUES (?,?,?,?) ON CONFLICT(notification_key) DO NOTHING
                        """)) {
                    insert.setString(1, key);
                    insert.setString(2, hash);
                    insert.setObject(3, UUID.randomUUID());
                    insert.setObject(4, eventId);
                    inserted = insert.executeUpdate();
                }
                try (var select = c.prepareStatement("""
                        SELECT canonical_payload_hash,acceptance_id
                        FROM kkref_r1_provider_stub.accepted_notifications WHERE notification_key=?
                        """)) {
                    select.setString(1, key);
                    try (var rows = select.executeQuery()) {
                        if (!rows.next()) { throw new IllegalStateException("Receipt not visible"); }
                        if (!hash.equals(rows.getString(1))) {
                            c.rollback();
                            reply(exchange, 409, "payload conflict");
                            return;
                        }
                        String receipt = rows.getString(2);
                        c.commit();
                        reply(exchange, inserted == 1 ? 201 : 200, receipt);
                    }
                }
            }
        } catch (Exception exception) {
            // Do not emit request payload, SQL, credentials, or token material.
            exchange.close();
        }
    }

    private static void reply(HttpExchange exchange, int status, String text) throws Exception {
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
