package org.koikifw.buildsupport.referencee2e;

import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Fresh-DB-only gate on the selected PUBLISHED -> PROCESSING update. */
final class R3DatabaseGate implements AutoCloseable {
    private static final long LOCK_KEY = 0x4b4b523341303147L;
    private final LocalNotificationProviderStub stub;
    private final Connection holder;
    private final UUID request;
    private final int holderPid;

    R3DatabaseGate(LocalNotificationProviderStub stub, UUID request) throws Exception {
        this.stub = stub;
        this.request = request;
        try (var connection = stub.admin(); var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA kkref_r3_gate");
            statement.execute("""
                    CREATE FUNCTION kkref_r3_gate.hold_target() RETURNS trigger AS $$
                    BEGIN
                      IF OLD.status = 'PUBLISHED' AND NEW.status = 'PROCESSING'
                         AND NEW.serialized_event::jsonb->>'expenseRequestId' = '%s' THEN
                        PERFORM pg_advisory_xact_lock(%d);
                      END IF;
                      RETURN NEW;
                    END;
                    $$ LANGUAGE plpgsql
                    """.formatted(request, LOCK_KEY));
            statement.execute("""
                    CREATE TRIGGER r3_hold_target BEFORE UPDATE ON kkref_notification_async.event_publication
                    FOR EACH ROW EXECUTE FUNCTION kkref_r3_gate.hold_target()
                    """);
        }
        holder = stub.admin();
        try (var statement = holder.prepareStatement("SELECT pg_advisory_lock(?)")) {
            statement.setLong(1, LOCK_KEY);
            statement.executeQuery().close();
            try (var check = holder.createStatement(); var rows = check.executeQuery("SELECT pg_backend_pid()")) {
                rows.next();
                holderPid = rows.getInt(1);
            }
            verifyInstalled();
        } catch (Exception failure) {
            holder.close();
            throw failure;
        }
    }

    int awaitBlockedBackend() throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(8));
        while (Instant.now().isBefore(deadline)) {
            try (var connection = stub.observer(); var statement = connection.prepareStatement("""
                    SELECT w.pid, a.wait_event_type, a.wait_event
                    FROM pg_locks h JOIN pg_locks w
                      ON h.locktype = w.locktype AND h.database = w.database
                     AND h.classid = w.classid AND h.objid = w.objid
                     AND h.objsubid = w.objsubid
                    JOIN pg_stat_activity a ON a.pid = w.pid
                    WHERE h.pid = ? AND h.locktype = 'advisory' AND h.granted
                      AND NOT w.granted AND w.pid <> h.pid
                      AND a.datname = current_database()
                    """)) {
                statement.setInt(1, holderPid);
                try (var result = statement.executeQuery()) {
                    if (result.next()) {
                        int pid = result.getInt(1);
                        String type = result.getString(2);
                        String event = result.getString(3);
                        if (result.next()) {
                            diagnostics("multiple-waiters");
                            throw new IllegalStateException("A01 advisory lock waiter is not unique");
                        }
                        if (!"Lock".equals(type) || !"advisory".equals(event)) {
                            diagnostics("wait-event-mismatch");
                            throw new IllegalStateException("A01 waiter activity does not confirm advisory lock");
                        }
                        verifyPreSend();
                        System.out.println("R3_A01_GATE holder_pid=" + holderPid + " waiter_pid=" + pid
                                + " wait_event=" + type + "/" + event);
                        return pid;
                    }
                }
            }
            Thread.sleep(50);
        }
        diagnostics("no-waiter");
        throw new IllegalStateException("A01 target update did not reach the DB gate");
    }

    private void verifyInstalled() throws Exception {
        try (var connection = stub.admin(); var statement = connection.createStatement();
                var rows = statement.executeQuery("""
                    SELECT pg_get_triggerdef(t.oid), pg_get_functiondef(t.tgfoid)
                    FROM pg_trigger t
                    WHERE t.tgrelid = 'kkref_notification_async.event_publication'::regclass
                      AND t.tgname = 'r3_hold_target' AND NOT t.tgisinternal
                    """)) {
            if (!rows.next() || !rows.getString(1).contains("BEFORE UPDATE")
                    || !rows.getString(2).contains(request.toString())
                    || !rows.getString(2).contains(Long.toString(LOCK_KEY)) || rows.next()) {
                throw new IllegalStateException("A01 target trigger definition mismatch");
            }
        }
        System.out.println("R3_A01_GATE trigger=verified request=" + request + " holder_pid=" + holderPid);
    }

    private void verifyPreSend() throws Exception {
        String row = targetSummary();
        long arrivals = stub.arrivalCount();
        long receipts = stub.receiptCount();
        System.out.println("R3_A01_GATE pre_send=" + row + " arrivals=" + arrivals + " receipts=" + receipts);
        if (!row.startsWith("PUBLISHED/") || arrivals != 0 || receipts != 0) {
            throw new IllegalStateException("A01 pre-send evidence mismatch");
        }
    }

    private String targetSummary() throws Exception {
        try (var connection = stub.observer(); var statement = connection.prepareStatement("""
                SELECT status, completion_attempts FROM kkref_notification_async.event_publication
                WHERE serialized_event::jsonb->>'expenseRequestId' = ?
                """)) {
            statement.setString(1, request.toString());
            try (var rows = statement.executeQuery()) {
                if (!rows.next()) return "absent";
                String summary = rows.getString(1) + "/" + rows.getInt(2);
                return rows.next() ? "multiple" : summary;
            }
        }
    }

    private void diagnostics(String reason) throws Exception {
        System.out.println("R3_A01_GATE blocked_reason=" + reason + " target=" + targetSummary()
                + " arrivals=" + stub.arrivalCount() + " receipts=" + stub.receiptCount());
        try (var connection = stub.observer(); var statement = connection.createStatement();
                var rows = statement.executeQuery("""
                    SELECT pid, state, wait_event_type, wait_event FROM pg_stat_activity
                    WHERE datname = current_database() AND pid <> pg_backend_pid()
                    ORDER BY pid LIMIT 8
                    """)) {
            while (rows.next()) {
                System.out.println("R3_A01_ACTIVITY pid=" + rows.getInt(1) + " state=" + rows.getString(2)
                        + " wait=" + rows.getString(3) + "/" + rows.getString(4));
            }
        }
    }

    @Override
    public void close() throws Exception {
        try {
            try (var statement = holder.prepareStatement("SELECT pg_advisory_unlock(?)")) {
                statement.setLong(1, LOCK_KEY);
                try (var result = statement.executeQuery()) {
                    result.next();
                    if (!result.getBoolean(1)) throw new IllegalStateException("A01 DB gate lock lost");
                }
            }
        } finally {
            holder.close();
            try (var connection = stub.admin(); var statement = connection.createStatement()) {
                statement.execute("DROP TRIGGER IF EXISTS r3_hold_target ON kkref_notification_async.event_publication");
                statement.execute("DROP SCHEMA IF EXISTS kkref_r3_gate CASCADE");
            }
        }
    }
}
