package org.koikifw.reference.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.koikifw.referenceacceptance.notification.NotificationFoundationDbHarness.Mode.READER;

import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.koikifw.reference.notification.application.RecoveryHoldException;
import org.koikifw.reference.notification.application.query.IsolatedRecoverySelection;
import org.koikifw.referenceacceptance.notification.NotificationFoundationDbHarness;
import org.koikifw.referenceacceptance.notification.PermittedRecoveryConnectionTestSupport;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Q01-Q07: real consume/commit and recovery code, bounded doubles below recovery. */
@EnabledIfSystemProperty(named = "koiki.reference.permit-recovery-connection.verification.enabled", matches = "true")
class PermittedSinglePublicationRecoveryTest {
    private static PostgreSQLContainer postgres;
    private static NotificationFoundationDbHarness db;
    private PermittedRecoveryConnectionTestSupport fixture;

    @BeforeAll static void start() throws Exception {
        // Check before starting any container, rather than discovering the missing envelope afterwards.
        assertThat(System.getProperty("koiki.reference.verification.resource-limits.enabled")).isEqualTo("true");
        assertThat(Runtime.getRuntime().maxMemory()).isEqualTo(805306368L);
        postgres = NotificationFoundationDbHarness.database();
        postgres.start();
        try { db = new NotificationFoundationDbHarness(postgres); }
        catch (Exception | AssertionError failure) { postgres.stop(); throw failure; }
    }

    @BeforeEach void prepare() throws Exception { fixture = new PermittedRecoveryConnectionTestSupport(db); }
    @AfterEach void cleanup() throws Exception { if (fixture != null) fixture.close(); }
    @AfterAll static void finish() {
        try { if (db != null) db.close(); }
        finally { if (postgres != null) postgres.stop(); }
    }

    @Test void commitsConsumptionBeforeOneRecoveryAndRejectsReplay() throws Exception {
        var connection = fixture.connection(false);
        fixture.run(connection);
        fixture.assertCounts(1, 1, 1);
        fixture.assertResult("RECOVERY_COMPLETED", "SUCCESS");
        assertThatThrownBy(() -> fixture.run(connection)).isInstanceOf(IllegalStateException.class);
        assertThat(fixture.entries.get()).isEqualTo(1);
        assertThat(fixture.resubmissions.get()).isEqualTo(1);
        fixture.assertCounts(1, 1, 1);
    }

    @Test void rejectsMismatchedTargetBeforeConsumption() {
        var selection = fixture.selection;
        var mismatched = new IsolatedRecoverySelection(selection.environmentId(), UUID.randomUUID(), selection.eventId(),
                selection.listenerId(), selection.status(), selection.completionAttempts(),
                selection.notificationKey(), selection.payloadSha256());
        var connection = fixture.connection(false);
        assertThatThrownBy(() -> connection.recover(fixture.permit, fixture.target, fixture.operation,
                PermittedRecoveryConnectionTestSupport.WORKER, mismatched)).isInstanceOf(IllegalArgumentException.class);
        fixture.assertCounts(0, 0, 0);
        assertThat(fixture.entries.get()).isZero();
        assertThat(fixture.resubmissions.get()).isZero();
    }

    @Test void doesNotRecoverWhenConsumptionAuditFails() {
        var connection = fixture.connection(false);
        fixture.revokeAudit();
        assertThatThrownBy(() -> fixture.run(connection)).isInstanceOf(RuntimeException.class);
        fixture.assertCounts(0, 0, 0);
        assertThat(fixture.entries.get()).isZero();
        assertThat(fixture.resubmissions.get()).isZero();
    }

    @Test void recoversOnlyOnceForTwoConcurrentConsumers() throws Exception {
        var connection = fixture.connection(false);
        var barrier = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);
        Future<Boolean> first = null;
        Future<Boolean> second = null;
        try {
            java.util.concurrent.Callable<Boolean> request = () -> {
                NotificationFoundationDbHarness.authenticate(NotificationFoundationDbHarness.USER);
                try {
                    barrier.await(30, TimeUnit.SECONDS);
                    try { fixture.run(connection); return true; }
                    catch (IllegalStateException expectedLoser) { return false; }
                } finally { SecurityContextHolder.clearContext(); }
            };
            first = executor.submit(request);
            second = executor.submit(request);
            boolean one = first.get(30, TimeUnit.SECONDS);
            boolean two = second.get(30, TimeUnit.SECONDS);
            assertThat(one ^ two).isTrue();
        } finally {
            if (first != null && !first.isDone()) first.cancel(true);
            if (second != null && !second.isDone()) second.cancel(true);
            executor.shutdownNow();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }
        fixture.assertCounts(1, 1, 1);
        assertThat(fixture.entries.get()).isEqualTo(1);
        assertThat(fixture.resubmissions.get()).isEqualTo(1);
        fixture.assertResult("RECOVERY_COMPLETED", "SUCCESS");
    }

    @Test void doesNotRecoverAfterCommittedConsumptionReturnsFailure() {
        var failing = fixture.connection(true);
        assertThatThrownBy(() -> fixture.run(failing)).isInstanceOf(IllegalStateException.class);
        fixture.assertCounts(1, 1, 0);
        db.close();
        NotificationFoundationDbHarness.authenticate(NotificationFoundationDbHarness.USER);
        var view = db.service(READER).read(fixture.permit, fixture.target.environmentId(), fixture.target.publicationId());
        assertThat(view.operationId()).isEqualTo(fixture.operation);
        assertThat(view.consumedAt()).isNotNull();
        var restarted = fixture.connection(false);
        assertThatThrownBy(() -> fixture.run(restarted)).isInstanceOf(IllegalStateException.class);
        fixture.assertCounts(1, 1, 0);
        assertThat(fixture.entries.get()).isZero();
        assertThat(fixture.resubmissions.get()).isZero();
    }

    @Test void doesNotRetryRecoveryAfterHold() throws Exception {
        fixture.hold = true;
        var connection = fixture.connection(false);
        assertThatThrownBy(() -> fixture.run(connection)).isInstanceOf(RecoveryHoldException.class);
        fixture.assertCounts(1, 1, 1);
        fixture.assertResult("RECOVERY_HELD", "FAILURE");
        assertThatThrownBy(() -> fixture.run(connection)).isInstanceOf(IllegalStateException.class);
        assertThat(fixture.entries.get()).isEqualTo(1);
        assertThat(fixture.resubmissions.get()).isZero();
        fixture.assertCounts(1, 1, 1);
    }

    @Test void doesNotRepeatRecoveryAfterResultAuditFails() {
        fixture.failResultAudit = true;
        var connection = fixture.connection(false);
        assertThatThrownBy(() -> fixture.run(connection)).isInstanceOf(RuntimeException.class);
        fixture.assertCounts(1, 1, 0);
        assertThatThrownBy(() -> fixture.run(connection)).isInstanceOf(IllegalStateException.class);
        assertThat(fixture.entries.get()).isEqualTo(1);
        assertThat(fixture.resubmissions.get()).isEqualTo(1);
        fixture.assertCounts(1, 1, 0);
    }
}
