package org.koikifw.buildsupport.referencee2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** R3-A selected paths through isolated packaged processes and a fresh provider ledger. */
@EnabledIfSystemProperty(named = "koiki.reference.async.r3.verification.enabled", matches = "true")
@Timeout(240)
class PackagedReferenceAsyncRecoveryTest {
    @Test
    void A01PublishedBeforeSendCompletesOneTargetAfterFullStop() throws Exception {
        try (var fixture = new R3RecoveryFixture()) {
            fixture.start(fixture.base.stub.endpoint());
            UUID request = fixture.base.submittedRequest(fixture.base.applicant);
            R3RecoveryTarget target;
            R3RecoveryFixture.StopReceipt stop;
            try (var gate = new R3DatabaseGate(fixture.base.stub, request)) {
                assertEquals(302, fixture.base.approveRequest(request, 2).statusCode());
                gate.awaitBlockedBackend();
                target = fixture.snapshot(request, fixture.base.applicant);
                assertEquals("PUBLISHED", target.status());
                assertEquals(0, fixture.base.stub.arrivalCount());
                assertEquals(0, fixture.base.stub.receiptCount());
                stop = fixture.stopOriginal();
            }
            assertEquals("PUBLISHED", fixture.snapshot(request, fixture.base.applicant).status());
            assertEquals(0, fixture.base.stub.arrivalCount());
            fixture.authorize(target);
            fixture.recover(target, stop);
            assertEquals(1, fixture.base.stub.arrivalCount());
        }
    }

    @Test
    void A02ProcessingBeforeProviderArrivalCompletesOneTargetAfterFullStop() throws Exception {
        try (var fixture = new R3RecoveryFixture();
                var gate = new R3ProviderGate(R3ProviderGate.Position.BEFORE_ARRIVAL,
                        fixture.base.stub.endpoint())) {
            fixture.start(gate.endpoint());
            UUID request = fixture.base.submittedRequest(fixture.base.applicant);
            assertEquals(302, fixture.base.approveRequest(request, 2).statusCode());
            gate.awaitGate();
            R3RecoveryTarget target = fixture.snapshot(request, fixture.base.applicant);
            assertEquals("PROCESSING", target.status());
            assertEquals(0, fixture.base.stub.arrivalCount());
            assertEquals(0, fixture.base.stub.receiptCount());
            var stop = fixture.stopOriginal();
            gate.abortAfterStop();
            fixture.authorize(target);
            fixture.recover(target, stop);
            assertEquals(1, fixture.base.stub.arrivalCount());
        }
    }

    @Test
    void A03AcceptedBeforeCompletionReusesOneProviderReceiptAfterFullStop() throws Exception {
        try (var fixture = new R3RecoveryFixture();
                var gate = new R3ProviderGate(R3ProviderGate.Position.AFTER_RECEIPT,
                        fixture.base.stub.endpoint())) {
            fixture.start(gate.endpoint());
            UUID request = fixture.base.submittedRequest(fixture.base.applicant);
            assertEquals(302, fixture.base.approveRequest(request, 2).statusCode());
            gate.awaitGate();
            R3RecoveryTarget target = fixture.snapshot(request, fixture.base.applicant);
            assertEquals("PROCESSING", target.status());
            assertEquals(1, fixture.base.stub.arrivalCount());
            assertEquals(1, fixture.base.stub.receiptCount());
            String receipt = fixture.base.stub.receipt(target.key());
            var stop = fixture.stopOriginal();
            gate.abortAfterStop();
            fixture.authorize(target);
            fixture.recover(target, stop);
            assertEquals(1, fixture.base.stub.arrivalCount(),
                    "R3-B accepted receipt must complete without a second POST");
            assertEquals(receipt, fixture.base.stub.receipt(target.key()));
        }
    }

    @Test
    void A04aLiveOriginalProcessRejectsRecoveryWithoutProviderRequest() throws Exception {
        try (var fixture = new R3RecoveryFixture();
                var gate = new R3ProviderGate(R3ProviderGate.Position.BEFORE_ARRIVAL,
                        fixture.base.stub.endpoint());
                var recoveryProbe = new R3RecoveryProviderProbe()) {
            fixture.start(gate.endpoint());
            UUID request = fixture.base.submittedRequest(fixture.base.applicant);
            assertEquals(302, fixture.base.approveRequest(request, 2).statusCode());
            gate.awaitGate();
            R3RecoveryTarget target = fixture.snapshot(request, fixture.base.applicant);
            fixture.authorize(target);
            var original = fixture.base.ownedProcess();
            assertTrue(original.isAlive(), "Original Reference must be running at recovery launch");
            var recovery = fixture.base.launchRecovery(fixture.asyncJar, target, recoveryProbe.endpoint(),
                    original.pid(), original.info().startInstant().orElseThrow(), false);
            assertTrue(recovery.waitFor(120, TimeUnit.SECONDS), "Rejected recovery process did not exit");
            assertTrue(recovery.exitValue() != 0, "Live-original recovery was accepted");
            assertTrue(Files.readString(fixture.base.recoveryLogPath())
                    .contains("Original Reference process is not confirmed stopped"));
            assertEquals(0, recoveryProbe.arrivalCount(), "Rejected recovery sent to provider");
            assertTrue(original.isAlive(), "Original Reference stopped before rejection was observed");
            gate.abortAfterStop();
        }
    }

    @Test
    void A04bStaleAttemptSnapshotRejectsRecoveryWithoutProviderRequest() throws Exception {
        try (var fixture = new R3RecoveryFixture();
                var gate = new R3ProviderGate(R3ProviderGate.Position.BEFORE_ARRIVAL,
                        fixture.base.stub.endpoint())) {
            fixture.start(gate.endpoint());
            UUID request = fixture.base.submittedRequest(fixture.base.applicant);
            assertEquals(302, fixture.base.approveRequest(request, 2).statusCode());
            gate.awaitGate();
            R3RecoveryTarget current = fixture.snapshot(request, fixture.base.applicant);
            var stop = fixture.stopOriginal();
            gate.abortAfterStop();
            fixture.authorize(current);
            var stale = new R3RecoveryTarget(current.environmentId(), current.requestId(),
                    current.publicationId(), current.eventId(), current.recipientId(),
                    current.listenerId(), current.status(),
                    current.attempts() + 1, current.key(), current.payloadHash());
            fixture.expectRejected(stale, stop.pid(), stop.start(), true);
            assertTrue(Files.readString(fixture.base.recoveryLogPath())
                    .contains("Isolated recovery target snapshot changed"));
            assertEquals(0, fixture.base.stub.arrivalCount());
            assertEquals(0, fixture.base.stub.receiptCount());
            assertEquals(current, fixture.snapshot(request, fixture.base.applicant));
        }
    }

    @Test
    void A04cOtherPublicationRejectsRecoveryWithoutProviderRequest() throws Exception {
        try (var fixture = new R3RecoveryFixture();
                var gate = new R3ProviderGate(R3ProviderGate.Position.BEFORE_ARRIVAL,
                        fixture.base.stub.endpoint())) {
            fixture.start(gate.endpoint());
            UUID first = fixture.base.submittedRequest(fixture.base.applicant);
            UUID other = fixture.base.submittedRequest(fixture.base.secondApplicant);
            assertEquals(302, fixture.base.approveRequest(first, 2).statusCode());
            gate.awaitGate();
            assertEquals(302, fixture.base.approveRequest(other, 2).statusCode());
            R3RecoveryTarget authorized = fixture.snapshot(first, fixture.base.applicant);
            R3RecoveryTarget notAuthorized = fixture.snapshot(other, fixture.base.secondApplicant);
            var stop = fixture.stopOriginal();
            gate.abortAfterStop();
            fixture.authorize(authorized);
            fixture.expectRejected(notAuthorized, stop.pid(), stop.start(), true);
            assertTrue(Files.readString(fixture.base.recoveryLogPath())
                    .contains("Isolated recovery target not authorized in fresh DB"));
            assertEquals(0, fixture.base.stub.arrivalCount());
            assertEquals(0, fixture.base.stub.receiptCount());
            assertEquals(authorized, fixture.snapshot(first, fixture.base.applicant));
            assertEquals(notAuthorized, fixture.snapshot(other, fixture.base.secondApplicant));
        }
    }

    @Test
    void A04dOccupiedAdvisoryLockRejectsSingleRecoveryBeforeProviderSend() throws Exception {
        try (var fixture = new R3RecoveryFixture();
                var gate = new R3ProviderGate(R3ProviderGate.Position.BEFORE_ARRIVAL,
                        fixture.base.stub.endpoint())) {
            fixture.start(gate.endpoint());
            UUID request = fixture.base.submittedRequest(fixture.base.applicant);
            assertEquals(302, fixture.base.approveRequest(request, 2).statusCode());
            gate.awaitGate();
            R3RecoveryTarget target = fixture.snapshot(request, fixture.base.applicant);
            var stop = fixture.stopOriginal();
            gate.abortAfterStop();
            fixture.authorize(target);
            // A Tooling session injects an occupied lock; only one recovery process is launched.
            try (var holder = fixture.base.stub.admin();
                    var lock = holder.prepareStatement("SELECT pg_advisory_lock(?)")) {
                lock.setLong(1, 0x4b4b524546523341L);
                lock.executeQuery().close();
                int holderBackendPid;
                try (var statement = holder.createStatement();
                        var held = statement.executeQuery("""
                        SELECT pg_backend_pid(), EXISTS (
                            SELECT 1 FROM pg_locks
                            WHERE pid = pg_backend_pid() AND locktype = 'advisory'
                              AND mode = 'ExclusiveLock' AND granted)
                        """)) {
                    assertTrue(held.next());
                    holderBackendPid = held.getInt(1);
                    assertTrue(holderBackendPid > 0, "Tooling lock holder backend must be identifiable");
                    assertTrue(held.getBoolean(2), "Tooling backend must hold an advisory lock");
                }
                try (var observer = fixture.base.stub.admin();
                        var probe = observer.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
                    probe.setLong(1, 0x4b4b524546523341L);
                    try (var result = probe.executeQuery()) {
                        assertTrue(result.next());
                        assertFalse(result.getBoolean(1), "The same lock key must already be occupied");
                    }
                }
                fixture.expectRejected(target, stop.pid(), stop.start(), true);
                try (var statement = holder.createStatement();
                        var held = statement.executeQuery("""
                        SELECT pg_backend_pid(), EXISTS (
                            SELECT 1 FROM pg_locks
                            WHERE pid = pg_backend_pid() AND locktype = 'advisory'
                              AND mode = 'ExclusiveLock' AND granted)
                        """)) {
                    assertTrue(held.next());
                    assertEquals(holderBackendPid, held.getInt(1));
                    assertTrue(held.getBoolean(2), "Tooling lock must stay held through rejection");
                }
                assertTrue(Files.readString(fixture.base.recoveryLogPath())
                        .contains("Isolated recovery lock unavailable"),
                        "Recovery must be rejected by the occupied lock, not an unrelated startup failure");
                assertEquals(0, fixture.base.stub.arrivalCount());
                assertEquals(0, fixture.base.stub.receiptCount());
                assertEquals(target, fixture.snapshot(request, fixture.base.applicant));
            }
            try (var observer = fixture.base.stub.admin();
                    var probe = observer.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
                probe.setLong(1, 0x4b4b524546523341L);
                try (var result = probe.executeQuery()) {
                    assertTrue(result.next());
                    assertTrue(result.getBoolean(1), "Tooling lock must be released after holder closes");
                }
                try (var release = observer.prepareStatement("SELECT pg_advisory_unlock(?)")) {
                    release.setLong(1, 0x4b4b524546523341L);
                    try (var result = release.executeQuery()) {
                        assertTrue(result.next());
                        assertTrue(result.getBoolean(1));
                    }
                }
            }
        }
    }
}
