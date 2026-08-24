package org.koikifw.archunit.fixture.gate3.business.rich.domain.repository;

import org.koikifw.archunit.fixture.gate3.business.rich.domain.model.RichModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.Repository;

public final class RepositoryFixtures {

    private RepositoryFixtures() {
    }

    public interface PlainRepository {
    }

    public interface JpaSpecificRepository extends JpaRepository<RichModel, Long> {
    }

    public interface ValidRepository extends Repository<RichModel, Long> {
    }
}
