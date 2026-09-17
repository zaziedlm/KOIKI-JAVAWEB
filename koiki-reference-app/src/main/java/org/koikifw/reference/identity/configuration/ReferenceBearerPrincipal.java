package org.koikifw.reference.identity.configuration;

import java.util.Set;
import org.koikifw.identity.AuthenticationSource;
import org.koikifw.identity.FrameworkPrincipal;
import org.koikifw.identity.FrameworkUserId;

/** Immutable Reference principal that does not expose JWT claims to application code. */
record ReferenceBearerPrincipal(FrameworkUserId userId, Set<String> permissions)
        implements FrameworkPrincipal {

    ReferenceBearerPrincipal {
        permissions = Set.copyOf(permissions);
    }

    @Override
    public AuthenticationSource authenticationSource() {
        return AuthenticationSource.BEARER;
    }
}
