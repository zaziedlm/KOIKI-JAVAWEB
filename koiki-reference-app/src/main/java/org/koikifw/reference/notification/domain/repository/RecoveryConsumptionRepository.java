package org.koikifw.reference.notification.domain.repository;

import java.util.Optional;
import java.util.UUID;
import org.koikifw.reference.notification.domain.model.RecoveryConsumption;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

/** Append-only port, to be used only after scoped permit lookup. */
@NoRepositoryBean
public interface RecoveryConsumptionRepository extends Repository<RecoveryConsumption, UUID> {
    Optional<RecoveryConsumption> findByPermitId(UUID permitId);
    void insert(RecoveryConsumption consumption);
}
