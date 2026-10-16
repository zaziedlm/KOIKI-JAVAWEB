package org.koikifw.reference.notification.application;

import java.util.Objects;
import java.util.UUID;
import org.koikifw.audit.AuditActor;
import org.koikifw.audit.AuditEvent;
import org.koikifw.audit.AuditResult;
import org.koikifw.audit.BusinessAuditRecorder;
import org.koikifw.identity.FrameworkPrincipal;
import org.koikifw.reference.notification.application.query.IsolatedRecoverySelection;
import org.koikifw.reference.notification.application.query.RecoveryTarget;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Reference-only connection fixture; deliberately not registered as a Bean or operational entry. */
public final class PermittedSinglePublicationRecoveryService {
    private final RecoveryPermitService permits;
    private final SinglePublicationRecoveryService recovery;
    private final BusinessAuditRecorder audit;
    private final TransactionTemplate resultTransaction;

    public PermittedSinglePublicationRecoveryService(RecoveryPermitService permits,
            SinglePublicationRecoveryService recovery, BusinessAuditRecorder audit,
            PlatformTransactionManager manager) {
        this.permits = Objects.requireNonNull(permits);
        this.recovery = Objects.requireNonNull(recovery);
        this.audit = Objects.requireNonNull(audit);
        resultTransaction = new TransactionTemplate(Objects.requireNonNull(manager));
        resultTransaction.setTimeout(10);
    }

    public void recover(UUID permitId, RecoveryTarget target, UUID operationId, String workerGeneration,
            IsolatedRecoverySelection selection) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Recovery connection requires a transaction-free boundary");
        }
        Objects.requireNonNull(permitId);
        Objects.requireNonNull(operationId);
        Objects.requireNonNull(workerGeneration);
        Objects.requireNonNull(target);
        Objects.requireNonNull(selection);
        if (!target.environmentId().equals(selection.environmentId())
                || !target.publicationId().equals(selection.publicationId())
                || !target.eventId().equals(selection.eventId())
                || !target.listenerId().equals(selection.listenerId())
                || target.expectedAttempt() != selection.completionAttempts()) {
            throw new IllegalArgumentException("Recovery selection mismatch");
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof FrameworkPrincipal principal)) {
            // Delegate the authoritative authentication/authorization decision and denial Audit.
            permits.consume(permitId, target, operationId, workerGeneration);
            throw new IllegalStateException("Recovery identity unavailable");
        }
        var actor = AuditActor.user(principal.userId().value().toString());
        // No recovery on exception, including a commit whose outcome is unknown to this caller.
        permits.consume(permitId, target, operationId, workerGeneration);
        try {
            recovery.recover(selection);
        } catch (RuntimeException failure) {
            try {
                record(actor, operationId, failure instanceof RecoveryHoldException ? "RECOVERY_HELD" : "RECOVERY_FAILED",
                        AuditResult.FAILURE);
            } catch (RuntimeException auditFailure) {
                failure.addSuppressed(auditFailure);
            }
            throw failure;
        }
        // Outside the catch: failure of this separate transaction never repeats recovery or its Audit.
        record(actor, operationId, "RECOVERY_COMPLETED", AuditResult.SUCCESS);
    }

    private void record(AuditActor actor, UUID operationId, String action, AuditResult result) {
        resultTransaction.executeWithoutResult(status -> audit.record(AuditEvent.of(
                "NOTIFICATION_RECOVERY_CHANGE", actor, action, result)
                .withResource("NOTIFICATION_RECOVERY_OPERATION", operationId.toString())));
    }
}
