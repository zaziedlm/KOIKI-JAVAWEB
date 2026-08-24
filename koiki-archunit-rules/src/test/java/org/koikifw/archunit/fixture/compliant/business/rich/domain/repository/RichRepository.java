package org.koikifw.archunit.fixture.compliant.business.rich.domain.repository;

import org.koikifw.archunit.fixture.compliant.business.rich.domain.model.RichAggregate;
import org.springframework.data.repository.Repository;

public interface RichRepository extends Repository<RichAggregate, Long> {

    RichAggregate getById(Long id);
}
