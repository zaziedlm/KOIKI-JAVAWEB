package org.koikifw.reference.identity.application;

import java.util.Optional;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.identity.IdentityAdministration;
import org.koikifw.identity.IdentityQuery;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/** Coordinates Reference identity management without accessing Framework persistence. */
@Service
@PreAuthorize("hasAuthority('IDENTITY:ADMIN')")
public class IdentityUserManagement {

    private final IdentityQuery identityQuery;
    private final IdentityAdministration identityAdministration;

    public IdentityUserManagement(
            IdentityQuery identityQuery, IdentityAdministration identityAdministration) {
        this.identityQuery = identityQuery;
        this.identityAdministration = identityAdministration;
    }

    /** Finds one user through the Framework-owned query contract. */
    public Optional<IdentityUserView> findUser(FrameworkUserId userId) {
        return identityQuery.findById(userId).map(IdentityUserView::from);
    }

    /** Assigns one existing Framework role using Framework-owned mutation semantics. */
    public void assignRole(FrameworkUserId userId, String roleCode, long expectedVersion) {
        identityAdministration.assignRole(userId, roleCode, expectedVersion);
    }

    /** Revokes one Framework role using Framework-owned mutation semantics. */
    public void revokeRole(FrameworkUserId userId, String roleCode, long expectedVersion) {
        identityAdministration.revokeRole(userId, roleCode, expectedVersion);
    }
}
