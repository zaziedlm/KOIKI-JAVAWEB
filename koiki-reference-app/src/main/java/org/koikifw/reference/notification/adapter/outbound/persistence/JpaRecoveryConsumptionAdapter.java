package org.koikifw.reference.notification.adapter.outbound.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.koikifw.reference.notification.domain.model.RecoveryConsumption;
import org.koikifw.reference.notification.domain.repository.RecoveryConsumptionRepository;

/** Explicit append-only JPA adapter; no update/delete entrypoint. */
public class JpaRecoveryConsumptionAdapter implements RecoveryConsumptionRepository {
    private final EntityManager entityManager;
    public JpaRecoveryConsumptionAdapter(EntityManager entityManager) { this.entityManager = entityManager; }
    @Override public Optional<RecoveryConsumption> findByPermitId(UUID id) {
        return persistence(() -> Optional.ofNullable(entityManager.find(RecoveryConsumption.class, id)));
    }
    @Override public void insert(RecoveryConsumption consumption) {
        persistence(() -> { entityManager.persist(consumption); return Boolean.TRUE; });
    }
    private static <T> T persistence(Supplier<T> operation) {
        try { return operation.get(); }
        catch (PersistenceException failure) {
            var translated = EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(failure);
            throw translated != null ? translated : failure;
        }
    }
}
