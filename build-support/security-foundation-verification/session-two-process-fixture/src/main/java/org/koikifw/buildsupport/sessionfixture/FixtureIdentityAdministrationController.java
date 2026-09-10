package org.koikifw.buildsupport.sessionfixture;

import java.util.Arrays;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.identity.IdentityAdministration;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class FixtureIdentityAdministrationController {

    private final IdentityAdministration identityAdministration;

    FixtureIdentityAdministrationController(IdentityAdministration identityAdministration) {
        this.identityAdministration = identityAdministration;
    }

    @GetMapping("/fixture/admin/csrf")
    String csrf(CsrfToken csrfToken) {
        return csrfToken.getToken();
    }

    @PostMapping("/fixture/admin/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void disable(@RequestParam String userId, @RequestParam long expectedVersion) {
        identityAdministration.disable(FrameworkUserId.parse(userId), expectedVersion);
    }

    @PostMapping("/fixture/admin/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void password(
            @RequestParam String userId,
            @RequestParam String password,
            @RequestParam long expectedVersion) {
        char[] rawPassword = password.toCharArray();
        try {
            identityAdministration.setLocalPassword(
                    FrameworkUserId.parse(userId), rawPassword, expectedVersion);
        } finally {
            Arrays.fill(rawPassword, '\0');
        }
    }

    @PostMapping("/fixture/admin/user-role")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void userRole(
            @RequestParam String userId,
            @RequestParam String roleCode,
            @RequestParam long expectedVersion) {
        identityAdministration.revokeRole(
                FrameworkUserId.parse(userId), roleCode, expectedVersion);
    }

    @PostMapping("/fixture/admin/role-permission")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void rolePermission(
            @RequestParam String roleCode,
            @RequestParam String permissionCode,
            @RequestParam long expectedRoleVersion) {
        identityAdministration.revokePermission(roleCode, permissionCode, expectedRoleVersion);
    }

    @PostMapping("/fixture/admin/external-unlink")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void externalUnlink(
            @RequestParam String userId,
            @RequestParam String issuer,
            @RequestParam long expectedVersion) {
        identityAdministration.unlinkExternalIdentity(
                FrameworkUserId.parse(userId), issuer, expectedVersion);
    }
}
