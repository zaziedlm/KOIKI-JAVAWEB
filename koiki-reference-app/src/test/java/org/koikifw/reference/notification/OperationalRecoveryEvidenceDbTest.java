package org.koikifw.reference.notification;

import static org.assertj.core.api.Assertions.*;
import static org.koikifw.referenceacceptance.notification.OperationalRecoveryTestSupport.*;
import static org.koikifw.referenceacceptance.notification.NotificationFoundationDbHarness.Mode.*;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.koikifw.reference.notification.application.RecoveryPermitService;
import org.koikifw.reference.notification.application.query.RecoveryTarget;
import org.koikifw.referenceacceptance.notification.ManagedRecoveryTestSupport;
import org.koikifw.referenceacceptance.notification.NotificationDbTest;
import org.koikifw.referenceacceptance.notification.NotificationFoundationDbHarness;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Real persistence/Identity/Audit; operational facts and post-commit execution decision remain test-owned. */
class OperationalRecoveryEvidenceDbTest extends NotificationDbTest {
    static final UUID OTHER_USER = UUID.fromString("76000000-0000-0000-0000-000000000001");
    static final UUID OTHER_ROLE = UUID.fromString("76000000-0000-0000-0000-000000000002");
    @TempDir Path directory;
    ConfigurableApplicationContext permitContext, consumerContext;
    RecoveryPermitService permits, consumer;
    Model model;
    RecoveryTarget target;
    UUID id, operation;
    Body body;

    @BeforeEach void connect() throws Exception {
        target = target(); model = new Model(db.clock, target);
        model.observed = new Observation(target, 1, true, new Window(FROM, FROM.plusSeconds(1800)));
        var path = ManagedRecoveryTestSupport.file(directory, ManagedRecoveryTestSupport.json(String.join(",",
                ManagedRecoveryTestSupport.grant("ISSUE"), ManagedRecoveryTestSupport.grant("READ"),
                ManagedRecoveryTestSupport.grant("EXECUTE"), ManagedRecoveryTestSupport.grant("CLOSE"),
                "{\"userId\":\"" + OTHER_USER + "\",\"capability\":\"NOTIFICATION:PERMIT:EXECUTE\",\"publicationId\":\""
                        + target.publicationId() + "\"}")));
        permitContext = db.open(PERMIT, false, ManagedRecoveryTestSupport.properties(path), false);
        consumerContext = db.open(CONSUMER, false, ManagedRecoveryTestSupport.properties(path), false);
        permits = service(permitContext, model); consumer = service(consumerContext, model);
        id = permits.issue(target, "TEST_REASON"); operation = UUID.randomUUID();
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        body = model.collect(id, target, operation, WORKER);
    }
    @AfterEach void disconnect() {
        try { if (consumerContext != null) consumerContext.close(); }
        finally { if (permitContext != null) permitContext.close(); SecurityContextHolder.clearContext(); }
    }
    void consume() { consumer.consume(id, target, operation, WORKER); }
    void noConsumption() throws Exception { assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_consumption")).containsExactly("0"); }
    void denied() throws Exception { assertThatThrownBy(this::consume).isInstanceOf(IllegalStateException.class).hasMessage("Recovery operation unavailable"); noConsumption(); }

    @Test void D01CommitsWithRealIdentityAuditAndTraceableEvidence() throws Exception {
        model.save(body);
        model.beforeEvidence = () -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            try {
                assertThat(db.rows("SELECT EXISTS (SELECT 1 FROM pg_locks WHERE relation='kkref_notification_recovery_permit'::regclass AND mode='RowShareLock' AND granted)")).containsExactly("true");
            } catch (Exception failure) { throw new IllegalStateException("Test lock observation failed", failure); }
        };
        consume(); assertThat(model.transactionObserved).isTrue();
        assertThat(db.rows("SELECT permit_id::text,operation_id::text,worker_generation FROM kkref_notification_recovery_consumption"))
                .containsExactly(id + "|" + operation + "|" + WORKER);
        assertThat(db.rows("SELECT count(*) FROM koiki_audit_event WHERE action='PERMIT_CONSUMED'")).containsExactly("1");
        assertThat(model.resolve(new EvidenceKey(target.environmentId(), id, operation, WORKER))).contains(body);
        assertThat(permits.read(id, target.environmentId(), target.publicationId()).operationId()).isEqualTo(operation);
    }
    @Test void D02RechecksCurrentIdentityAndIssuer() throws Exception {
        model.save(body); db.removeCapability("EXECUTE");
        assertThatThrownBy(this::consume).isInstanceOf(IllegalStateException.class).hasMessage("Recovery access denied"); noConsumption();
        NotificationFoundationDbHarness.authenticate(UUID.randomUUID());
        assertThatThrownBy(this::consume).isInstanceOf(IllegalStateException.class).hasMessage("Recovery access denied"); noConsumption();
        assertThat(db.rows("SELECT count(*) FROM koiki_audit_event WHERE action='PERMIT_ACCESS_DENIED'")).containsExactly("2");
        db.sql("INSERT INTO koiki_user(user_id,email,canonical_email,status) VALUES ('" + OTHER_USER + "','other@test.invalid','other@test.invalid','ACTIVE')");
        db.sql("INSERT INTO koiki_role(role_id,role_code) VALUES ('" + OTHER_ROLE + "','OTHER_TEST_OPERATOR')");
        db.sql("INSERT INTO koiki_user_role VALUES ('" + OTHER_USER + "','" + OTHER_ROLE + "')");
        db.sql("INSERT INTO koiki_role_permission SELECT '" + OTHER_ROLE + "',permission_id FROM koiki_permission WHERE permission_code='NOTIFICATION:PERMIT:EXECUTE'");
        NotificationFoundationDbHarness.authenticate(OTHER_USER);
        var current = consumerContext.getBean(org.koikifw.reference.notification.application.port.outbound.RecoveryIdentityPort.class)
                .current(org.koikifw.identity.FrameworkUserId.parse(OTHER_USER.toString())).orElseThrow();
        assertThat(current.status()).isEqualTo(org.koikifw.identity.UserStatus.ACTIVE);
        assertThat(current.permissionCodes()).contains("NOTIFICATION:PERMIT:EXECUTE");
        assertThat(consumerContext.getBean(org.koikifw.reference.notification.application.port.outbound.RecoveryScopePort.class)
                .check(OTHER_USER, "NOTIFICATION:PERMIT:EXECUTE", target.environmentId(), target.publicationId()))
                .isEqualTo(org.koikifw.reference.notification.application.port.outbound.RecoveryScopePort.Decision.ALLOWED);
        denied(); // Authorization passed; execution still requires the actual issuer. No worker delegation is inferred.
    }
    @Test void D03RejectsCurrentTargetChanges() throws Exception {
        model.save(body); var previous = model.observed;
        model.observed = new Observation(target, 2, true, previous.window()); denied();
        model.observed = null; denied();
        var next = new RecoveryTarget(target.environmentId(), target.publicationId(), target.eventId(), target.listenerId(), 1);
        model.observed = new Observation(next, 1, true, previous.window()); denied();
    }
    @Test void D04RejectsRestartAndStopControlLoss() throws Exception {
        model.save(body); model.restarted = true; denied();
        model.restarted = false; model.controlAvailable = false; denied();
    }
    @Test void D05CannotTurnProviderUncertaintyIntoConsumption() throws Exception {
        for (var state : new Acceptance[] { Acceptance.ACCEPTED, Acceptance.UNKNOWN, Acceptance.CONTRADICTORY }) {
            model.provider = model.receipt("test-logical-notification", "test-payload-1", "test-recipient", state);
            operation = UUID.randomUUID(); body = model.collect(id, target, operation, WORKER); model.save(body); denied();
        }
    }
    @Test void D06RejectsUnstoredLostAndExpiredEvidence() throws Exception {
        model.saveFails = true; assertThatThrownBy(() -> model.save(body)).isInstanceOf(IllegalStateException.class); denied();
        model.saveFails = false; model.save(body); model.storeAvailable = false; denied(); model.storeAvailable = true;
        model.beforeEvidence = () -> db.clock.now = FROM.plusSeconds(60); denied();
    }
    @Test void D07AuditFailureRollsBackWithoutInferringEvidenceUse() throws Exception {
        model.save(body); db.sql("REVOKE INSERT ON koiki_audit_event FROM kkref_notification_consumer");
        try { denied(); } finally { db.sql("GRANT INSERT ON koiki_audit_event TO kkref_notification_consumer"); }
        assertThat(db.rows("SELECT version FROM kkref_notification_recovery_permit")).containsExactly("0");
        assertThat(db.rows("SELECT count(*) FROM koiki_audit_event WHERE action='PERMIT_CONSUMED'")).containsExactly("0");
        assertThat(model.resolve(body.key())).contains(body); // Saved evidence is present, but DB shows it was not used.
    }
    @Test void D08CommitsOneConsumptionUnderCompetition() throws Exception {
        model.save(body); var ready = new CountDownLatch(2); var go = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Integer> task = () -> {
                NotificationFoundationDbHarness.authenticate(USER); ready.countDown();
                try {
                    if (!go.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Test barrier timeout");
                    try { consume(); return 1; } catch (IllegalStateException rejected) { return 0; }
                } finally { SecurityContextHolder.clearContext(); }
            };
            var first = executor.submit(task); var second = executor.submit(task);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); go.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS) + second.get(10, TimeUnit.SECONDS)).isEqualTo(1);
        } finally { go.countDown(); executor.shutdownNow(); assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue(); }
        assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_consumption")).containsExactly("1");
        assertThat(db.rows("SELECT count(*) FROM koiki_audit_event WHERE action='PERMIT_CONSUMED'")).containsExactly("1");
    }
    @Test void D09KeepsConsumptionAfterPostCommitHold() throws Exception {
        model.save(body); consume();
        assertThat(model.executionCandidate(body, false)).isFalse(); // Commit-unknown model does not permit execution.
        model.controlAvailable = false; assertThat(model.executionCandidate(body, true)).isFalse();
        assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_consumption")).containsExactly("1");
        assertThat(model.resolve(body.key())).contains(body); // No sender exists in this test.
    }
    @Test void D10ClosesOnlyReconciledHumanResolution() throws Exception {
        model.save(body); consume();
        model.provider = model.receipt("test-logical-notification", "test-payload-1", "test-recipient", Acceptance.UNKNOWN);
        model.saveResolution(new Resolution(id, target, USER, "unknown", "INVESTIGATE", model.provider, window()));
        assertThatThrownBy(() -> permits.close(id, target, "unknown")).isInstanceOf(IllegalStateException.class);
        assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_permit WHERE closed_at IS NOT NULL")).containsExactly("0");
        model.provider = model.accept("test-logical-notification", "test-payload-1", "test-recipient");
        model.saveResolution(new Resolution(id, target, USER, "reconciled", "PROVIDER_RECONCILED", model.provider, window()));
        permits.close(id, target, "reconciled");
        assertThat(db.rows("SELECT result_ref FROM kkref_notification_recovery_permit")).containsExactly("reconciled");
        assertThat(db.rows("SELECT count(*) FROM koiki_audit_event WHERE action='PERMIT_CLOSED'")).containsExactly("1");
        assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_consumption")).containsExactly("1");
    }
    @Test void D11PreservesExistingUniquenessAndRecordsHistoricalClosureLimit() throws Exception {
        assertThatThrownBy(() -> permits.issue(target, "TEST_REASON")).isInstanceOf(IllegalStateException.class);
        model.save(body); consume();
        assertThatThrownBy(() -> permits.issue(target, "TEST_REASON")).isInstanceOf(IllegalStateException.class);
        var original = model.observed;
        var next = new RecoveryTarget(target.environmentId(), target.publicationId(), target.eventId(), target.listenerId(), 1);
        model.provider = model.accept("test-logical-notification", "test-payload-1", "test-recipient");
        model.saveResolution(new Resolution(id, target, USER, "historical", "PROVIDER_RECONCILED", model.provider, window()));
        model.observed = new Observation(next, 1, true, original.window());
        assertThatThrownBy(() -> permits.close(id, target, "historical")).isInstanceOf(IllegalStateException.class);
        model.observed = null; assertThatThrownBy(() -> permits.close(id, target, "historical")).isInstanceOf(IllegalStateException.class);
        model.observed = original; db.clock.now = FROM.plusSeconds(301);
        assertThatThrownBy(() -> permits.issue(target, "TEST_REASON")).isInstanceOf(IllegalStateException.class);
        assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_permit WHERE closed_at IS NULL")).containsExactly("1");
        assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_consumption")).containsExactly("1");
        // Uniqueness is on environment/publication, not a logical-notification key across publications.
        assertThat(db.rows("SELECT indexdef FROM pg_indexes WHERE indexname='uk_kkref_notification_permit_unclosed_target'"))
                .singleElement().asString().contains("environment_id, publication_id").doesNotContain("event_id");
    }
    @Test void D12DistinguishesDetectedRevocationFromPostCheckGap() throws Exception {
        model.save(body); model.beforeEvidence = () -> model.controlAvailable = false; denied();
        model.beforeEvidence = () -> { }; model.controlAvailable = true;
        model.afterEvidence = () -> model.controlAvailable = false;
        consume(); // Known gap: state changed after the last affirmative check; no production fencing is asserted.
        assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_consumption")).containsExactly("1");
        assertThat(model.executionCandidate(body, true)).isFalse();
    }
}
