package org.koikifw.archunit.fixture.gate3.business.rich.adapter.inbound.mvc;

import org.koikifw.archunit.fixture.gate3.business.rich.domain.model.RichModel;
import org.springframework.web.bind.annotation.GetMapping;

public final class InboundApiFixtures {

    private InboundApiFixtures() {
    }

    public static RichModel exposedApi(RichModel model) {
        return model;
    }

    @GetMapping("/argument")
    public static String mappedArgument(RichModel model) {
        return model.id().toString();
    }

    @GetMapping("/return")
    public static RichModel mappedReturn() {
        return new RichModel(2L);
    }
}
