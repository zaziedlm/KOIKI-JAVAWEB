package org.koikifw.reference.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.koikifw.referenceacceptance.notification.ManagedRecoveryTestSupport.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.koikifw.reference.notification.application.RecoveryPermitService;
import org.koikifw.reference.notification.application.port.outbound.RecoveryTtlPolicyPort;
import org.koikifw.reference.notification.application.query.RecoveryTarget;
import org.koikifw.referenceacceptance.notification.NotificationDbTest;
import org.koikifw.referenceacceptance.notification.NotificationFoundationDbHarness.Mode;

class ManagedRecoveryConfigurationDbTest extends NotificationDbTest {
    @TempDir Path directory;
    private Path configuration() throws Exception { return file(directory, json(String.join(",", grant("ISSUE"), grant("READ"), grant("EXECUTE"), grant("CLOSE")))); }
    private RecoveryTarget prepareTarget() { var target = target(); db.ports.snapshots.put(target.publicationId(), target); return target; }
    @Test void D01CommitsPermitWithRealIdentityAndAudit() throws Exception {
        var path = configuration(); var target = prepareTarget();
        try (var context = db.open(Mode.PERMIT, false, properties(path), true)) {
            var id = context.getBean(RecoveryPermitService.class).issue(target, "TEST_REASON");
            assertThat(db.rows("SELECT actor_id::text FROM kkref_notification_recovery_permit WHERE permit_id='" + id + "'")).containsExactly(USER.toString());
            assertThat(db.rows("SELECT count(*) FROM koiki_audit_event WHERE action='PERMIT_ISSUED'")).containsExactly("1");
            var view = context.getBean(RecoveryPermitService.class).read(id, ENV, PUBLICATION);
            assertThat(java.time.Duration.between(view.issuedAt(), view.expiresAt())).isEqualTo(java.time.Duration.ofMinutes(5));
        }
    }
    @Test void D02RejectsCurrentlyRevokedIdentityCapability() throws Exception {
        var path = configuration(); var target = prepareTarget();
        try (var context = db.open(Mode.PERMIT, false, properties(path), true)) {
            db.removeCapability("ISSUE");
            assertThatThrownBy(() -> context.getBean(RecoveryPermitService.class).issue(target, "TEST_REASON")).isInstanceOf(IllegalStateException.class);
            assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_permit")).containsExactly("0");
        }
    }
    @Test void D03RejectsScopeWithSecurityAuditAndNoStorage() throws Exception {
        var path = configuration(); var target = db.target();
        try (var context = db.open(Mode.PERMIT, false, properties(path), true)) {
            assertThatThrownBy(() -> context.getBean(RecoveryPermitService.class).issue(target, "TEST_REASON")).isInstanceOf(IllegalStateException.class);
            assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_permit")).containsExactly("0");
            assertThat(db.rows("SELECT count(*) FROM koiki_audit_event WHERE action='PERMIT_ACCESS_DENIED'")).containsExactly("1");
        }
    }
    @Test void D04RollsBackWhenAuditGrantIsMissing() throws Exception {
        var path = configuration(); var target = prepareTarget();
        try (var context = db.open(Mode.PERMIT, false, properties(path), true)) {
            db.sql("REVOKE INSERT ON koiki_audit_event FROM kkref_notification_permit");
            try { assertThatThrownBy(() -> context.getBean(RecoveryPermitService.class).issue(target, "TEST_REASON")).isInstanceOf(IllegalStateException.class); }
            finally { db.sql("GRANT INSERT ON koiki_audit_event TO kkref_notification_permit"); }
            assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_permit")).containsExactly("0");
        }
    }
    @Test void D05RechecksIssueInstantBeforeWriting() throws Exception {
        var path = configuration(); var target = prepareTarget();
        try (var context = db.open(Mode.PERMIT, false, properties(path), true)) {
            var actual = context.getBean(RecoveryTtlPolicyPort.class);
            // Test-owned decorator moves time after a successful TTL lookup; the actual adapter must reject at issuance.
            RecoveryTtlPolicyPort crossing = new RecoveryTtlPolicyPort() {
                @Override public java.util.Optional<java.time.Duration> durationFor(RecoveryTarget t) { var result = actual.durationFor(t); db.clock.now = FROM.plusSeconds(1800); return result; }
                @Override public boolean allowsIssuanceAt(RecoveryTarget t, Instant issued, Instant expires) { return actual.allowsIssuanceAt(t, issued, expires); }
            };
            var service = new RecoveryPermitService(context.getBean(org.koikifw.reference.notification.domain.repository.RecoveryPermitRepository.class),
                    context.getBean(org.koikifw.reference.notification.domain.repository.RecoveryConsumptionRepository.class),
                    context.getBean(org.koikifw.reference.notification.application.port.outbound.RecoveryIdentityPort.class),
                    context.getBean(org.koikifw.reference.notification.application.port.outbound.RecoveryScopePort.class), crossing,
                    context.getBean(org.koikifw.reference.notification.application.port.outbound.RecoveryTargetPort.class),
                    context.getBean(org.koikifw.reference.notification.application.port.outbound.RecoveryEvidencePort.class),
                    context.getBean(org.koikifw.audit.BusinessAuditRecorder.class), context.getBean(org.koikifw.audit.SecurityAuditRecorder.class),
                    db.clock, context.getBean(org.springframework.transaction.PlatformTransactionManager.class));
            assertThatThrownBy(() -> service.issue(target, "TEST_REASON")).isInstanceOf(IllegalStateException.class);
            assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_permit")).containsExactly("0");
            assertThat(db.rows("SELECT count(*) FROM koiki_audit_event")).containsExactly("0");
        }
    }
    @Test void D06RetainsConsumptionAndExpiresOnlyAfterSnapshotReplacement() throws Exception {
        var path = configuration(); var manifest = properties(path); var target = prepareTarget(); UUID id; String expiry;
        try (var permit = db.open(Mode.PERMIT, false, manifest, true)) {
            var service = permit.getBean(RecoveryPermitService.class); id = service.issue(target, "TEST_REASON");
            expiry = db.rows("SELECT expires_at::text FROM kkref_notification_recovery_permit").getFirst();
            Files.writeString(path, json(grant("READ")));
            // File changes cannot silently revoke or replace the startup snapshot.
            assertThat(service.read(id, ENV, PUBLICATION).permitId()).isEqualTo(id);
            try (var consumer = db.open(Mode.CONSUMER, false, properties(file(directory, json(String.join(",", grant("EXECUTE"), grant("READ"))))), true)) {
                var operation = UUID.randomUUID(); db.proof(id, target, operation, "managed-test-worker");
                consumer.getBean(RecoveryPermitService.class).consume(id, target, operation, "managed-test-worker");
            }
        }
        file(directory, json(grant("READ"))); // Reconfiguration after both old contexts have closed.
        try (var replacement = db.open(Mode.PERMIT, false, properties(path), true)) {
            assertThatThrownBy(() -> replacement.getBean(RecoveryPermitService.class).close(id, target, "test-proof")).isInstanceOf(IllegalStateException.class);
            assertThat(db.rows("SELECT expires_at::text FROM kkref_notification_recovery_permit")).containsExactly(expiry);
            assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_consumption")).containsExactly("1");
            db.clock.now = FROM.plusSeconds(1800);
            assertThatThrownBy(() -> replacement.getBean(RecoveryPermitService.class).read(id, ENV, PUBLICATION)).isInstanceOf(IllegalStateException.class);
        }
    }
}
