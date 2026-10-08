package org.koikifw.reference.notification.application;

import java.util.UUID;
import org.koikifw.reference.notification.application.port.outbound.RecoveryIssueProtectionPort;
import org.koikifw.reference.notification.application.query.RecoveryPermitView;
import org.koikifw.reference.notification.application.query.RecoveryTarget;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Keeps the supplier protection scope outside the existing owning transaction. */
public final class ProtectedRecoveryIssueService {
    private final RecoveryPermitService permits;
    private final RecoveryIssueProtectionPort protection;
    public ProtectedRecoveryIssueService(RecoveryPermitService permits,RecoveryIssueProtectionPort protection) {
        this.permits=permits;this.protection=protection;
    }
    public UUID issue(RecoveryTarget target,String reasonCode) {
        if(TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Protected recovery requires a transaction-free boundary");
        try(var scope=protection.open(target)) {
            if(!scope.active() || !target.equals(scope.target())) throw new IllegalStateException("Recovery protection unavailable");
            // This call returns only after its owning transaction has completed.
            return permits.issue(target,reasonCode);
        }
    }
    public RecoveryPermitView read(UUID permit,String environment,UUID publication) {
        return permits.read(permit,environment,publication);
    }
}
