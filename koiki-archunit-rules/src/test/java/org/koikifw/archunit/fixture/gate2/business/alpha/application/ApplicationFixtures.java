package org.koikifw.archunit.fixture.gate2.business.alpha.application;

import org.koikifw.archunit.fixture.gate2.business.alpha.adapter.inbound.web.InboundFixtures;
import org.koikifw.archunit.fixture.gate2.business.beta.application.BetaApplicationFixtures;
import org.koikifw.archunit.fixture.gate2.business.beta.domain.event.EventFixtures;

public final class ApplicationFixtures {

    private ApplicationFixtures() {
    }

    public static final class AlphaUseCase {
    }

    public static final class DependsOnInbound {
        public InboundFixtures.DependsOnOutbound inbound() {
            return null;
        }
    }

    public static final class DependsOnBetaApplication {
        public BetaApplicationFixtures.BetaUseCase betaUseCase() {
            return null;
        }
    }

    public static final class DependsOnBetaEvent {
        public EventFixtures.AllowedEvent event() {
            return null;
        }
    }

    public static final class UsesRestTemplate {
        public org.springframework.web.client.RestTemplate restTemplate() {
            return null;
        }
    }
}
