package org.koikifw.archunit.fixture.gate2.business.alpha.domain.repository;

import org.springframework.data.repository.Repository;

public final class RepositoryFixtures {

    private RepositoryFixtures() {
    }

    public interface AlphaRepository extends Repository<Object, Long> {
    }
}
