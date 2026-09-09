package org.koikifw.reference.identity.adapter.inbound.web;

import org.koikifw.identity.FrameworkUserId;
import org.koikifw.reference.identity.application.IdentityUserManagement;
import org.koikifw.reference.identity.application.IdentityUserView;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

/** Receives the minimal Reference identity lookup journey. */
@Controller
public class IdentityManagementController {

    private static final String INVALID_USER_ID = "Invalid user identifier.";
    private static final String USER_NOT_FOUND = "Identity user was not found.";

    private final IdentityUserManagement identityUserManagement;

    public IdentityManagementController(IdentityUserManagement identityUserManagement) {
        this.identityUserManagement = identityUserManagement;
    }

    @GetMapping(path = "/identity/users", params = "!userId")
    String lookupForm() {
        return "identity/users";
    }

    @GetMapping(path = "/identity/users", params = "userId")
    RedirectView lookup(@RequestParam String userId) {
        FrameworkUserId parsed = parseUserId(userId);
        return new RedirectView("/identity/users/" + parsed);
    }

    @GetMapping("/identity/users/{userId}")
    String detail(@PathVariable String userId, Model model) {
        FrameworkUserId parsed = parseUserId(userId);
        IdentityUserView user = identityUserManagement
                .findUser(parsed)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND));
        model.addAttribute("user", user);
        return "identity/user-detail";
    }

    private static FrameworkUserId parseUserId(String userId) {
        try {
            return FrameworkUserId.parse(userId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_USER_ID);
        }
    }
}
