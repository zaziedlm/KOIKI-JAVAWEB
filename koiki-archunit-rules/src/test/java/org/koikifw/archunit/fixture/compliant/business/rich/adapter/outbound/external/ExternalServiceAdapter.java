package org.koikifw.archunit.fixture.compliant.business.rich.adapter.outbound.external;

import org.koikifw.archunit.fixture.compliant.business.rich.domain.gateway.ExternalService;

public final class ExternalServiceAdapter implements ExternalService {

    @Override
    public String lookup(long id) {
        return Long.toString(id);
    }
}
