package org.koikifw.reference;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Entry point for the Reference browser journeys. */
@Controller
public class HomeController {

    private final Resource spaIndex;

    HomeController(ResourceLoader resources) {
        this.spaIndex = resources.getResource("classpath:/META-INF/resources/spa/index.html");
    }

    @GetMapping("/")
    String home(Model model) {
        model.addAttribute("spaAvailable", spaIndex.exists());
        return "home";
    }
}
