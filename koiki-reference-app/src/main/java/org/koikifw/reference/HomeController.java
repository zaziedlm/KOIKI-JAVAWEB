package org.koikifw.reference;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Entry point for the Reference browser journeys. */
@Controller
public class HomeController {

    @GetMapping("/")
    String home() {
        return "home";
    }
}
