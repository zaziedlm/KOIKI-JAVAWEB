package org.koikifw.reference.notification.application.port.outbound;

import java.util.Optional;
import java.util.UUID;
import org.koikifw.reference.notification.application.query.RecoveryTarget;

/** Trusted resolution of stopping/reconciliation evidence. Empty must retain HOLD. */
public interface RecoveryEvidencePort {
    record ConsumptionProof(UUID permitId, RecoveryTarget target, UUID operationId,
            String workerGeneration, String evidenceRef) { }
    record ClosureProof(UUID permitId, RecoveryTarget target, UUID confirmedBy, String resultRef) { }
    Optional<ConsumptionProof> consumption(UUID permitId, RecoveryTarget target, UUID operationId, String workerGeneration);
    Optional<ClosureProof> closure(UUID permitId, RecoveryTarget target, UUID confirmedBy, String resultRef);
}
