package org.koikifw.buildsupport.referencee2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** One fresh DB and one package-owned Reference generation per R3-A case. */
final class R3RecoveryFixture implements AutoCloseable {
    private static final Pattern EVENT_ID = Pattern.compile("\\\"eventId\\\"\\s*:\\s*\\\"([0-9a-f-]{36})\\\"");
    final AsyncReferenceFixture base = new AsyncReferenceFixture();
    final String environmentId = "r3-" + UUID.randomUUID();
    final Path asyncJar = Path.of(System.getProperty("koiki.reference.async.verification.async-jar"));

    R3RecoveryFixture() throws Exception {
        assertTrue(Files.isRegularFile(asyncJar));
        try (var stream = Files.newInputStream(asyncJar)) {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = new byte[8192];
            int count;
            while ((count = stream.read(bytes)) != -1) digest.update(bytes, 0, count);
            System.out.println("R3_ARTIFACT sha256=" + HexFormat.of().formatHex(digest.digest()));
        }
    }

    void start(URI endpoint) throws Exception {
        base.launch(asyncJar, true, false, endpoint);
        base.readyAndSeed(true);
    }

    R3RecoveryTarget snapshot(UUID request, UUID applicant) throws Exception {
        try (var connection = base.stub.observer(); var statement = connection.prepareStatement("""
                SELECT id, listener_id, event_type, serialized_event, status,
                       completion_attempts, completion_date
                FROM kkref_notification_async.event_publication
                WHERE serialized_event::jsonb->>'expenseRequestId' = ?
                """)) {
            statement.setString(1, request.toString());
            try (var result = statement.executeQuery()) {
                assertTrue(result.next(), "R3 target publication absent");
                UUID id = result.getObject(1, UUID.class);
                String listener = result.getString(2);
                assertTrue(listener.contains("ExpenseApprovalListener.onApproval"));
                assertEquals("org.koikifw.reference.expense.domain.event.ExpenseApprovedEvent", result.getString(3));
                String serialized = result.getString(4);
                var matcher = EVENT_ID.matcher(serialized);
                assertTrue(matcher.find(), "R3 event ID absent");
                UUID event = UUID.fromString(matcher.group(1));
                assertTrue(serialized.contains(applicant.toString()));
                String status = result.getString(5);
                int attempts = result.getInt(6);
                assertFalse(result.wasNull());
                assertEquals(null, result.getTimestamp(7));
                assertFalse(result.next(), "R3 target publication not unique");
                String key = "expense-approved:v1:" + request + ":3:applicant";
                String payload = "{\"schemaVersion\":1,\"expenseRequestId\":\"" + request
                        + "\",\"applicantUserId\":\"" + applicant
                        + "\",\"approvalVersion\":3,\"claimedAmount\":1200}";
                return new R3RecoveryTarget(environmentId, request, id, event, listener, status,
                        attempts, key, LocalNotificationProviderStub.sha256(payload));
            }
        }
    }

    void authorize(R3RecoveryTarget target) throws Exception {
        try (var connection = base.stub.admin(); var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA kkref_r3_recovery");
            statement.execute("""
                    CREATE TABLE kkref_r3_recovery.authorized_target (
                      environment_id TEXT NOT NULL, publication_id UUID PRIMARY KEY,
                      event_id UUID NOT NULL)
                    """);
        }
        try (var connection = base.stub.admin(); var statement = connection.prepareStatement("""
                INSERT INTO kkref_r3_recovery.authorized_target
                  (environment_id, publication_id, event_id) VALUES (?, ?, ?)
                """)) {
            statement.setString(1, target.environmentId());
            statement.setObject(2, target.publicationId());
            statement.setObject(3, target.eventId());
            assertEquals(1, statement.executeUpdate());
        }
    }

    StopReceipt stopOriginal() throws Exception {
        var process = base.ownedProcess();
        long pid = process.pid();
        Instant start = process.info().startInstant().orElseThrow();
        assertTrue(process.isAlive(), "Original Reference exited before controlled stop");
        process.destroyForcibly();
        assertTrue(process.waitFor(5, TimeUnit.SECONDS), "Original Reference did not stop in 5 seconds");
        assertFalse(process.isAlive());
        assertTrue(ProcessHandle.of(pid).isEmpty(), "Original PID still present");
        return new StopReceipt(pid, start, process.exitValue());
    }

    void recover(R3RecoveryTarget target, StopReceipt stop) throws Exception {
        var process = base.launchRecovery(asyncJar, target, base.stub.endpoint(), stop.pid(), stop.start());
        assertTrue(process.waitFor(150, TimeUnit.SECONDS), "Recovery process deadline");
        if (process.exitValue() != 0) {
            throw new AssertionError("Isolated recovery process failed; inspect bounded child log "
                    + base.recoveryLogPath());
        }
        assertCompleted(target);
    }

    void expectRejected(R3RecoveryTarget target, long previousPid, Instant previousStart,
            boolean requireStopped) throws Exception {
        long before = base.stub.arrivalCount();
        var process = base.launchRecovery(asyncJar, target, base.stub.endpoint(),
                previousPid, previousStart, requireStopped);
        assertTrue(process.waitFor(120, TimeUnit.SECONDS), "Rejected recovery process did not exit");
        assertTrue(process.exitValue() != 0, "Invalid recovery was accepted");
        assertEquals(before, base.stub.arrivalCount(), "Rejected recovery sent to provider");
    }

    void assertCompleted(R3RecoveryTarget target) throws Exception {
        try (var connection = base.stub.observer(); var statement = connection.prepareStatement("""
                SELECT status, completion_attempts, completion_date
                FROM kkref_notification_async.event_publication WHERE id = ?
                """)) {
            statement.setObject(1, target.publicationId());
            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals("COMPLETED", result.getString(1));
                assertEquals(target.attempts() + 1, result.getInt(2));
                assertTrue(result.getTimestamp(3) != null);
                assertFalse(result.next());
            }
        }
        assertEquals(1, base.stub.receiptCount());
        assertEquals(1, base.successfulAuditCount(target.requestId(), "APPROVE_EXPENSE"));
        base.assertState(target.requestId(), "APPROVED", 3);
    }

    @Override
    public void close() throws Exception {
        base.close();
    }

    record StopReceipt(long pid, Instant start, int exitCode) { }
}
