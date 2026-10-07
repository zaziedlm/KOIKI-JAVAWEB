package org.koikifw.reference.notification.application.port.outbound;

import java.util.Optional;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.identity.IdentityUser;

public interface RecoveryIdentityPort {
    Optional<IdentityUser> current(FrameworkUserId userId);
}
