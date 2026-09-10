package org.koikifw.reference.identity.application;

import java.util.List;
import java.util.Objects;
import org.koikifw.identity.IdentityUser;
import org.koikifw.identity.UserStatus;

/** Immutable Reference-owned view of one Framework identity user. */
public record IdentityUserView(
        String userId,
        String email,
        UserStatus status,
        List<String> roleCodes,
        List<String> permissionCodes,
        long version) {

    public IdentityUserView {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(status, "status");
        roleCodes = List.copyOf(roleCodes);
        permissionCodes = List.copyOf(permissionCodes);
    }

    static IdentityUserView from(IdentityUser user) {
        Objects.requireNonNull(user, "user");
        return new IdentityUserView(
                user.userId().toString(),
                user.email(),
                user.status(),
                user.roleCodes().stream().sorted().toList(),
                user.permissionCodes().stream().sorted().toList(),
                user.version());
    }
}
