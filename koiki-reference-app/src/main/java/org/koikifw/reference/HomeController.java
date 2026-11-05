package org.koikifw.reference;

import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.core.Authentication;
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
    String home(Model model, @Nullable Authentication authentication) {
        model.addAttribute("spaAvailable", spaIndex.exists());
        model.addAttribute("canApplyExpenses", hasPermission(authentication, "EXPENSE:APPLY"));
        model.addAttribute("canApproveExpenses", hasPermission(authentication, "EXPENSE:APPROVE"));
        return "home";
    }

    private static boolean hasPermission(@Nullable Authentication authentication, String permission) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> permission.equals(authority.getAuthority()));
    }
}
