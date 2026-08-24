package org.koikifw.archunit.fixture.gate3.business.rich.application;

import org.koikifw.archunit.fixture.gate3.business.rich.domain.gateway.ExternalGateway;

public final class MisplacedGateway implements ExternalGateway {

    @Override
    public String request() {
        return "bad";
    }
}
