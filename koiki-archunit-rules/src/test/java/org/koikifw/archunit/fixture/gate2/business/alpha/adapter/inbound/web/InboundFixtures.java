package org.koikifw.archunit.fixture.gate2.business.alpha.adapter.inbound.web;

import org.koikifw.archunit.fixture.gate2.business.alpha.adapter.outbound.persistence.OutboundFixtures;
import org.koikifw.archunit.fixture.gate2.business.alpha.domain.repository.RepositoryFixtures;
import org.springframework.stereotype.Controller;

public final class InboundFixtures {

    private InboundFixtures() {
    }

    public static final class DependsOnOutbound {
        public OutboundFixtures.OutboundAdapter outboundAdapter() {
            return null;
        }
    }

    public static final class RepositoryCallingController {
        public RepositoryFixtures.AlphaRepository repository() {
            return null;
        }
    }

    @Controller
    public static final class AnnotatedEndpoint {
        public RepositoryFixtures.AlphaRepository repository() {
            return null;
        }
    }
}
