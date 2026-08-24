package org.koikifw.archunit.fixture.gate2.business.beta.domain.event;

import java.util.List;
import org.koikifw.archunit.fixture.gate2.business.beta.domain.model.DomainFixtures;

public final class EventFixtures {

    private EventFixtures() {
    }

    public record AllowedEvent(String value) {
    }

    public static final class MutableEvent {
    }

    public record LeakyEvent(DomainFixtures.BetaModel model) {
    }

    public record GenericLeakyEvent(List<DomainFixtures.BetaModel> models) {
    }
}
