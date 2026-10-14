package org.koikifw.reference.notification.adapter.outbound.identity;

import java.util.Optional;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.identity.IdentityQuery;
import org.koikifw.identity.IdentityUser;
import org.koikifw.reference.notification.application.port.outbound.RecoveryIdentityPort;

/** Uses only the public, current-value Identity boundary. */
public class PublicRecoveryIdentityAdapter implements RecoveryIdentityPort {
    private final IdentityQuery query;
    public PublicRecoveryIdentityAdapter(IdentityQuery query) { this.query = query; }
    @Override public Optional<IdentityUser> current(FrameworkUserId id) { return query.findById(id); }
}
