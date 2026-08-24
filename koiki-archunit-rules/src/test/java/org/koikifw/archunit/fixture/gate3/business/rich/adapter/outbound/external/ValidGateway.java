package org.koikifw.archunit.fixture.gate3.business.rich.adapter.outbound.external;

import org.koikifw.archunit.fixture.gate3.business.rich.domain.gateway.ExternalGateway;

public final class ValidGateway implements ExternalGateway {

    @Override
    public String request() {
        return "ok";
    }
}
