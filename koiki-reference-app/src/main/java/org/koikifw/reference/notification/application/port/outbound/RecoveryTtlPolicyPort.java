package org.koikifw.reference.notification.application.port.outbound;

import java.time.Duration;
import java.util.Optional;
import org.koikifw.reference.notification.application.query.RecoveryTarget;

public interface RecoveryTtlPolicyPort {
    Optional<Duration> durationFor(RecoveryTarget target);
}
