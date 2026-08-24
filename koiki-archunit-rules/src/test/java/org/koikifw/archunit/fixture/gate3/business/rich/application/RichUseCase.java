package org.koikifw.archunit.fixture.gate3.business.rich.application;

import org.koikifw.archunit.fixture.gate3.business.rich.domain.model.RichModel;

public final class RichUseCase {

    public RichModel load() {
        return new RichModel(1L);
    }
}
