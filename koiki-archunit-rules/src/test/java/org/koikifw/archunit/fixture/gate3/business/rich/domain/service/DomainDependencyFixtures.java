package org.koikifw.archunit.fixture.gate3.business.rich.domain.service;

import jakarta.persistence.EntityManager;
import org.koikifw.archunit.fixture.gate3.business.rich.adapter.outbound.persistence.PersistenceAdapter;
import org.springframework.web.context.request.WebRequest;

public final class DomainDependencyFixtures {

    private DomainDependencyFixtures() {
    }

    public static PersistenceAdapter adapterDependency() {
        return null;
    }

    public static EntityManager entityManagerDependency() {
        return null;
    }

    public static WebRequest webDependency() {
        return null;
    }
}
