package org.koikifw.archunit.fixture.v1.violation.rich.domain.service;

import jakarta.persistence.EntityManager;
import org.koikifw.archunit.fixture.v1.violation.rich.adapter.outbound.RichOutbound;

public class InvalidDomainService {
    private RichOutbound outbound;
    private EntityManager entityManager;
}
