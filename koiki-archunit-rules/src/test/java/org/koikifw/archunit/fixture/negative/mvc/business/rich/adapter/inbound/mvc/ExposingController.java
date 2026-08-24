package org.koikifw.archunit.fixture.negative.mvc.business.rich.adapter.inbound.mvc;

import org.koikifw.archunit.fixture.negative.mvc.business.rich.domain.model.ExposedModel;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

public final class ExposingController {

    @GetMapping("/exposed")
    public ExposedModel expose(ExposedModel input, Model model) {
        model.addAttribute("exposed", new ExposedModel());
        return input;
    }
}
