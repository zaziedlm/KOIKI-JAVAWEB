package org.koikifw.archunit.fixture.compliant.business.rich.adapter.inbound.mvc;

import org.koikifw.archunit.fixture.compliant.business.rich.application.RichUseCase;
import org.koikifw.archunit.fixture.compliant.business.rich.domain.model.RichAggregate;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

public final class RichController {

    private final RichUseCase useCase;

    public RichController(RichUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/rich")
    public String show(Long id, Model model) {
        RichAggregate aggregate = useCase.load(id);
        RichView view = new RichView(aggregate.id(), aggregate.name());
        model.addAttribute("rich", view);
        return "rich";
    }
}
