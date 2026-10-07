package org.koikifw.reference.notification.application.port.outbound;

import java.util.Optional;
import java.util.UUID;
import org.koikifw.reference.notification.application.query.RecoveryTarget;

public interface RecoveryTargetPort {
    Optional<RecoveryTarget> current(String environmentId, UUID publicationId);
}
