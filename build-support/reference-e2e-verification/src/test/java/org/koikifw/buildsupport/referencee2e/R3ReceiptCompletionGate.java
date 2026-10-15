package org.koikifw.buildsupport.referencee2e;

import java.sql.Connection;
import java.time.Instant;
import java.util.UUID;

/** Fresh-DB fault gate after durable receipt observation, before publication completion. */
final class R3ReceiptCompletionGate implements AutoCloseable {
    private static final long LOCK_KEY = 0x4b4b523342303147L;
    private final LocalNotificationProviderStub stub;
    private final Connection holder;
    private final int holderPid;

    R3ReceiptCompletionGate(LocalNotificationProviderStub stub, UUID request) throws Exception {
        this.stub = stub;
        try (var c = stub.admin(); var s = c.createStatement()) {
            s.execute("CREATE SCHEMA kkref_r3b_gate");
            s.execute("""
                    CREATE FUNCTION kkref_r3b_gate.hold_completion() RETURNS trigger AS $$
                    BEGIN
                      IF NEW.completion_date IS NOT NULL AND OLD.completion_date IS NULL
                         AND NEW.serialized_event::jsonb->>'expenseRequestId' = '%s' THEN
                        PERFORM pg_advisory_xact_lock(%d);
                      END IF;
                      RETURN NEW;
                    END;
                    $$ LANGUAGE plpgsql
                    """.formatted(request, LOCK_KEY));
            s.execute("""
                    CREATE TRIGGER r3b_hold_completion BEFORE UPDATE ON kkref_notification_async.event_publication
                    FOR EACH ROW EXECUTE FUNCTION kkref_r3b_gate.hold_completion()
                    """);
        }
        holder = stub.admin();
        try (var s = holder.prepareStatement("SELECT pg_advisory_lock(?)")) {
            s.setLong(1, LOCK_KEY);
            s.executeQuery().close();
            try (var check = holder.createStatement(); var result = check.executeQuery("SELECT pg_backend_pid()")) {
                result.next();
                holderPid = result.getInt(1);
            }
        }
    }

    void awaitBlockedWithReceipt(String key) throws Exception {
        Instant deadline = Instant.now().plusSeconds(15);
        while (Instant.now().isBefore(deadline)) {
            try (var c = stub.observer(); var s = c.prepareStatement("""
                    SELECT count(*) FROM pg_locks h JOIN pg_locks w
                      ON h.locktype=w.locktype AND h.database=w.database
                     AND h.classid=w.classid AND h.objid=w.objid AND h.objsubid=w.objsubid
                    WHERE h.pid=? AND h.locktype='advisory' AND h.granted
                      AND NOT w.granted AND w.pid<>h.pid
                    """)) {
                s.setInt(1, holderPid);
                try (var rows = s.executeQuery()) {
                    rows.next();
                    if (rows.getLong(1) == 1) {
                        try (var observation = c.prepareStatement("""
                                SELECT acceptance_id FROM kkref_notification_async.notification_receipt_observation
                                WHERE notification_key=?
                                """)) {
                            observation.setString(1, key);
                            try (var observed = observation.executeQuery()) {
                                if (!observed.next() || observed.getObject(1, UUID.class) == null || observed.next()) {
                                    throw new IllegalStateException("Receipt observation not durable at completion gate");
                                }
                            }
                        }
                        if (stub.receiptCount() != 1 || stub.arrivalCount() != 1) {
                            throw new IllegalStateException("Provider receipt evidence mismatch at completion gate");
                        }
                        return;
                    }
                }
            }
            Thread.sleep(50);
        }
        throw new IllegalStateException("Completion did not reach R3-B gate");
    }

    @Override
    public void close() throws Exception {
        try {
            try (var s = holder.prepareStatement("SELECT pg_advisory_unlock(?)")) {
                s.setLong(1, LOCK_KEY);
                try (var result = s.executeQuery()) {
                    result.next();
                    if (!result.getBoolean(1)) throw new IllegalStateException("R3-B gate lock lost");
                }
            }
        } finally {
            holder.close();
            try (var c = stub.admin(); var s = c.createStatement()) {
                s.execute("DROP TRIGGER IF EXISTS r3b_hold_completion ON kkref_notification_async.event_publication");
                s.execute("DROP SCHEMA IF EXISTS kkref_r3b_gate CASCADE");
            }
        }
    }
}
