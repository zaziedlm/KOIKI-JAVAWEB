package org.koikifw.identity;

import java.util.Objects;
import java.util.Set;

/** Read-only application view of a framework user and effective authorization. */
public record IdentityUser(
        FrameworkUserId userId,
        String email,
        UserStatus status,
        Set<String> roleCodes,
        Set<String> permissionCodes,
        long version) {

    public IdentityUser {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(status, "status");
        roleCodes = Set.copyOf(roleCodes);
        permissionCodes = Set.copyOf(permissionCodes);
    }
}
