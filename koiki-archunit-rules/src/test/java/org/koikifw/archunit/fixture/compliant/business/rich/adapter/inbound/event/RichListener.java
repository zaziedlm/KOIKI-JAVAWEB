package org.koikifw.archunit.fixture.compliant.business.rich.adapter.inbound.event;

import org.koikifw.archunit.fixture.compliant.business.rich.application.RichUseCase;
import org.koikifw.archunit.fixture.compliant.business.rich.domain.event.RichCompleted;
import org.springframework.context.event.EventListener;

public final class RichListener {

    private final RichUseCase useCase;

    public RichListener(RichUseCase useCase) {
        this.useCase = useCase;
    }

    @EventListener
    public void on(RichCompleted event) {
        useCase.load(event.id());
    }
}
