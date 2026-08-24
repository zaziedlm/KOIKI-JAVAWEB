package org.koikifw.archunit.fixture.compliant.business.simple.application;

import org.koikifw.archunit.fixture.compliant.business.rich.domain.event.RichCompleted;
import org.koikifw.archunit.fixture.compliant.business.simple.adapter.outbound.persistence.SimpleStore;

public final class SimpleUseCase {

    private final SimpleStore store;

    public SimpleUseCase(SimpleStore store) {
        this.store = store;
    }

    public void handle(RichCompleted event) {
        store.save(event.id());
    }
}
