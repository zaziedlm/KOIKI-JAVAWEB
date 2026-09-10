package org.koikifw.identity;

import java.util.Optional;

/** Reads framework-owned identity state without exposing persistence technology. */
public interface IdentityQuery {

    Optional<IdentityUser> findById(FrameworkUserId userId);
}
