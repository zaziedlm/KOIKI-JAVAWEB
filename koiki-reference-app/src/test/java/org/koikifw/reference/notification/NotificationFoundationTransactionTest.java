package org.koikifw.reference.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.koikifw.referenceacceptance.notification.NotificationFoundationDbHarness.Mode.*;
import static org.koikifw.referenceacceptance.notification.NotificationFoundationDbHarness.USER;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.koikifw.audit.AuditActor;
import org.koikifw.audit.AuditEvent;
import org.koikifw.audit.AuditResult;
import org.koikifw.audit.BusinessAuditRecorder;
import org.koikifw.reference.notification.application.port.outbound.RecoveryScopePort;
import org.koikifw.referenceacceptance.notification.NotificationDbTest;
import org.koikifw.referenceacceptance.notification.NotificationFoundationDbHarness;

/** Adopted T01-T16, with real public Query/Recorders and restricted runtime connections. */
class NotificationFoundationTransactionTest extends NotificationDbTest {
    @Test void commitsIssueWithBusinessAudit() throws Exception {
        var target = db.target(); var id = db.issue(target);
        assertThat(db.rows("SELECT actor_id::text FROM kkref_notification_recovery_permit WHERE permit_id='" + id + "'"))
                .containsExactly(USER.toString());
        assertAudit("PERMIT_ISSUED", id);
    }
    @Test void commitsConsumptionWithBusinessAudit() throws Exception {
        var target = db.target(); var id = db.issue(target); var operation = UUID.randomUUID();
        db.proof(id, target, operation, "test-worker");
        db.service(CONSUMER).consume(id, target, operation, "test-worker");
        var view = db.service(READER).read(id, target.environmentId(), target.publicationId());
        assertThat(view.operationId()).isEqualTo(operation);
        assertThat(view.workerGeneration()).isEqualTo("test-worker");
        assertThat(view.closedAt()).isNull();
        assertAudit("PERMIT_CONSUMED", id);
    }
    @Test void commitsClosureWithBusinessAudit() throws Exception {
        var target = db.target(); var id = db.issue(target); db.closure(id, target, "test-proof");
        db.service(PERMIT).close(id, target, "test-proof");
        var view = db.service(READER).read(id, target.environmentId(), target.publicationId());
        assertThat(view.confirmedBy()).isEqualTo(USER);
        assertThat(view.resultRef()).isEqualTo("test-proof");
        assertThat(view.version()).isEqualTo(1);
        assertAudit("PERMIT_CLOSED", id);
    }
    @Test void rollsBackIssueWhenAuditFails() throws Exception {
        var target = db.target();
        revokeAudit("kkref_notification_permit", () -> assertThatThrownBy(() -> db.issue(target)).isInstanceOf(RuntimeException.class));
        assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_permit")).containsExactly("0");
        assertThat(db.rows("SELECT count(*) FROM koiki_audit_event")).containsExactly("0");
    }
    @Test void rollsBackConsumptionWhenAuditFails() throws Exception {
        var target = db.target(); var id = db.issue(target); var operation = UUID.randomUUID(); db.proof(id, target, operation, "test-worker");
        revokeAudit("kkref_notification_consumer", () -> assertThatThrownBy(() -> db.service(CONSUMER).consume(id, target, operation, "test-worker"))
                .isInstanceOf(RuntimeException.class));
        assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_consumption")).containsExactly("0");
        assertThat(db.rows("SELECT version FROM kkref_notification_recovery_permit")).containsExactly("0");
        assertThat(db.rows("SELECT count(*) FROM koiki_audit_event WHERE action='PERMIT_CONSUMED'")).containsExactly("0");
    }
    @Test void rollsBackClosureWhenAuditFails() throws Exception {
        var target = db.target(); var id = db.issue(target); db.closure(id, target, "test-proof");
        revokeAudit("kkref_notification_permit", () -> assertThatThrownBy(() -> db.service(PERMIT).close(id, target, "test-proof"))
                .isInstanceOf(RuntimeException.class));
        assertThat(db.rows("SELECT closed_at,confirmed_by,result_ref,version FROM kkref_notification_recovery_permit"))
                .containsExactly("null|null|null|0");
        assertThat(db.rows("SELECT count(*) FROM koiki_audit_event WHERE action='PERMIT_CLOSED'")).containsExactly("0");
    }
    @Test void rejectsIssueWithoutCurrentCapability() throws Exception {
        var target = db.target(); db.removeCapability("ISSUE");
        assertDenied(() -> db.issue(target), "USER", "ACCESS_DENIED");
    }
    @Test void rejectsReadWithoutCurrentCapability() throws Exception {
        var target = db.target(); var id = db.issue(target); db.removeCapability("READ");
        assertDenied(() -> db.service(READER).read(id, target.environmentId(), target.publicationId()), "USER", "ACCESS_DENIED");
    }
    @Test void rejectsConsumptionWithoutCurrentCapability() throws Exception {
        var target = db.target(); var id = db.issue(target); var operation = UUID.randomUUID(); db.proof(id, target, operation, "test-worker");
        db.removeCapability("EXECUTE");
        assertDenied(() -> db.service(CONSUMER).consume(id, target, operation, "test-worker"), "USER", "ACCESS_DENIED");
        assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_consumption")).containsExactly("0");
    }
    @Test void rejectsClosureWithoutCurrentCapability() throws Exception {
        var target = db.target(); var id = db.issue(target); db.closure(id, target, "test-proof"); db.removeCapability("CLOSE");
        assertDenied(() -> db.service(PERMIT).close(id, target, "test-proof"), "USER", "ACCESS_DENIED");
    }
    @Test void rejectsDisabledIdentity() throws Exception {
        var target = db.target(); db.sql("UPDATE koiki_user SET status='DISABLED' WHERE user_id='" + USER + "'");
        assertDenied(() -> db.issue(target), "USER", "ACCESS_DENIED");
    }
    @Test void rejectsMissingIdentity() throws Exception {
        var target = db.target(); NotificationFoundationDbHarness.authenticate(UUID.randomUUID());
        assertDenied(() -> db.issue(target), "ANONYMOUS", "ACCESS_DENIED");
    }
    @Test void rejectsWhenIdentityQueryFails() throws Exception {
        var target = db.target(); db.context(PERMIT);
        db.sql("REVOKE SELECT ON koiki_user FROM kkref_notification_permit");
        try { assertDenied(() -> db.issue(target), "ANONYMOUS", "AUTHORIZATION_UNAVAILABLE"); }
        finally { db.sql("GRANT SELECT ON koiki_user TO kkref_notification_permit"); }
    }
    @Test void rejectsOutsideScopeWithoutLeakingTarget() throws Exception {
        var target = db.target(); var id = db.issue(target); db.ports.decision = RecoveryScopePort.Decision.OUTSIDE;
        assertDenied(() -> db.service(READER).read(id, target.environmentId(), target.publicationId()), "USER", "ACCESS_DENIED");
        assertThatThrownBy(() -> db.service(READER).read(UUID.randomUUID(), target.environmentId(), target.publicationId()))
                .isInstanceOf(IllegalStateException.class).hasMessage("Recovery access denied");
        db.ports.decision = RecoveryScopePort.Decision.UNAVAILABLE;
        assertDenied(() -> db.service(READER).read(id, target.environmentId(), target.publicationId()), "USER", "AUTHORIZATION_UNAVAILABLE");
    }
    @Test void rejectsBusinessAuditOutsideTransaction() throws Exception {
        var recorder = db.context(PERMIT).getBean(BusinessAuditRecorder.class);
        assertThatThrownBy(() -> recorder.record(AuditEvent.of("NOTIFICATION_RECOVERY_CHANGE", AuditActor.user(USER.toString()),
                "PERMIT_ISSUED", AuditResult.SUCCESS))).isInstanceOf(RuntimeException.class);
        assertThat(db.rows("SELECT count(*) FROM koiki_audit_event")).containsExactly("0");
    }
    @Test void preservesDenialWhenSecurityAuditFails() throws Exception {
        var target = db.target(); var id = db.issue(target); db.removeCapability("READ");
        revokeAudit("kkref_notification_reader", () -> assertThatThrownBy(() -> db.service(READER).read(id, target.environmentId(), target.publicationId()))
                .isInstanceOf(IllegalStateException.class).hasMessage("Recovery access denied"));
        assertThat(db.rows("SELECT count(*) FROM koiki_audit_event WHERE audit_type='SECURITY'")).containsExactly("0");
        assertThat(db.rows("SELECT count(*) FROM kkref_notification_recovery_permit")).containsExactly("1");
    }
    private void assertAudit(String action, UUID id) throws Exception {
        assertThat(db.rows("SELECT audit_type,event_type,actor_type,actor_id,resource_type,resource_id,result FROM koiki_audit_event WHERE action='" + action + "'"))
                .containsExactly("BUSINESS|NOTIFICATION_RECOVERY_CHANGE|USER|" + USER + "|NOTIFICATION_RECOVERY_PERMIT|" + id + "|SUCCESS");
    }
    private void assertDenied(Runnable action, String actor, String reason) throws Exception {
        assertThatThrownBy(action::run).isInstanceOf(IllegalStateException.class).hasMessage("Recovery access denied");
        assertThat(db.rows("SELECT actor_type,reason_code,resource_type,resource_id FROM koiki_audit_event WHERE audit_type='SECURITY' ORDER BY occurred_at"))
                .contains(actor + "|" + reason + "|null|null");
    }
    private void revokeAudit(String role, Runnable operation) throws Exception {
        db.sql("REVOKE INSERT ON koiki_audit_event FROM " + role);
        try { operation.run(); } finally { db.sql("GRANT INSERT ON koiki_audit_event TO " + role); }
    }
}
