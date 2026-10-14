package org.koikifw.reference.notification.domain.repository;

import java.util.Optional;
import java.util.UUID;
import org.koikifw.reference.notification.domain.model.RecoveryPermit;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

/** Narrow port; discovery must not register this interface while the foundation is disabled. */
@NoRepositoryBean
public interface RecoveryPermitRepository extends Repository<RecoveryPermit, UUID> {
    Optional<RecoveryPermit> findScoped(UUID permitId, String environmentId, UUID publicationId);
    Optional<RecoveryPermit> lockScoped(UUID permitId, String environmentId, UUID publicationId);
    void insert(RecoveryPermit permit);
    void advanceVersion(RecoveryPermit permit);
    void flush();
}
