package org.koikifw.reference.identity.application;

import java.util.Optional;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.identity.IdentityQuery;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/** Coordinates Reference identity management without accessing Framework persistence. */
@Service
@PreAuthorize("hasAuthority('IDENTITY:ADMIN')")
public class IdentityUserManagement {

    private final IdentityQuery identityQuery;

    public IdentityUserManagement(IdentityQuery identityQuery) {
        this.identityQuery = identityQuery;
    }

    /** Finds one user through the Framework-owned query contract. */
    public Optional<IdentityUserView> findUser(FrameworkUserId userId) {
        return identityQuery.findById(userId).map(IdentityUserView::from);
    }
}
