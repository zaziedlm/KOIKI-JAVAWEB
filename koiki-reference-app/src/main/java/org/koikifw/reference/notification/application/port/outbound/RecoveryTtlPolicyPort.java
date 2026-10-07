package org.koikifw.reference.notification.application.port.outbound;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.koikifw.reference.notification.application.query.RecoveryTarget;

public interface RecoveryTtlPolicyPort {
    Optional<Duration> durationFor(RecoveryTarget target);
    /** Recheck the policy at the actual issue time; unconnected policies never authorize issuance. */
    default boolean allowsIssuanceAt(RecoveryTarget target, Instant issuedAt, Instant expiresAt) { return false; }
}
