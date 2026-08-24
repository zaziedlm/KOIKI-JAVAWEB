package org.koikifw.archunit.fixture.compliant.business.rich.application;

import org.koikifw.archunit.fixture.compliant.business.rich.domain.gateway.ExternalService;
import org.koikifw.archunit.fixture.compliant.business.rich.domain.model.RichAggregate;
import org.koikifw.archunit.fixture.compliant.business.rich.domain.repository.RichRepository;

public final class RichUseCase {

    private final RichRepository repository;
    private final ExternalService externalService;

    public RichUseCase(RichRepository repository, ExternalService externalService) {
        this.repository = repository;
        this.externalService = externalService;
    }

    public RichAggregate load(Long id) {
        RichAggregate aggregate = repository.getById(id);
        externalService.lookup(id);
        return aggregate;
    }
}
