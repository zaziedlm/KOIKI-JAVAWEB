package org.koikifw.reference.notification.adapter.outbound.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.koikifw.reference.notification.domain.model.RecoveryPermit;
import org.koikifw.reference.notification.domain.repository.RecoveryPermitRepository;

/** Explicitly registered JPA adapter. Scope is applied in SQL before materialization. */
public class JpaRecoveryPermitAdapter implements RecoveryPermitRepository {
    private final EntityManager entityManager;

    public JpaRecoveryPermitAdapter(EntityManager entityManager) { this.entityManager = entityManager; }

    @Override
    public Optional<RecoveryPermit> findScoped(UUID id, String environment, UUID publication) {
        return persistence(() -> lookup(id, environment, publication, false));
    }

    @Override
    public Optional<RecoveryPermit> lockScoped(UUID id, String environment, UUID publication) {
        return persistence(() -> lookup(id, environment, publication, true));
    }

    private Optional<RecoveryPermit> lookup(UUID id, String environment, UUID publication, boolean lock) {
        var query = entityManager.createQuery("""
                select p from RecoveryPermit p where p.permitId = :id
                and p.environmentId = :environment and p.publicationId = :publication
                """, RecoveryPermit.class)
                .setParameter("id", id).setParameter("environment", environment).setParameter("publication", publication);
        if (lock) query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        return query.getResultList().stream().findFirst();
    }

    @Override public void insert(RecoveryPermit permit) {
        persistence(() -> { entityManager.persist(permit); return Boolean.TRUE; });
    }
    @Override public void advanceVersion(RecoveryPermit permit) {
        persistence(() -> { entityManager.lock(permit, LockModeType.PESSIMISTIC_FORCE_INCREMENT); return Boolean.TRUE; });
    }
    @Override public void flush() {
        persistence(() -> { entityManager.flush(); return Boolean.TRUE; });
    }

    private static <T> T persistence(Supplier<T> operation) {
        try { return operation.get(); }
        catch (PersistenceException failure) {
            var translated = EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(failure);
            throw translated != null ? translated : failure;
        }
    }
}
