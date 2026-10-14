package org.koikifw.reference.notification.adapter.outbound.configuration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.koikifw.reference.notification.application.port.outbound.RecoveryTtlPolicyPort;
import org.koikifw.reference.notification.application.query.RecoveryTarget;

public final class ManagedRecoveryTtlPolicyAdapter implements RecoveryTtlPolicyPort {
    private final ManagedRecoverySnapshot snapshot;
    private final Clock clock;
    public ManagedRecoveryTtlPolicyAdapter(ManagedRecoverySnapshot snapshot, Clock clock) { this.snapshot = snapshot; this.clock = clock; }
    @Override public Optional<Duration> durationFor(RecoveryTarget target) {
        var now = clock.instant();
        try { return allowsIssuanceAt(target, now, now.plus(snapshot.permitTtl())) ? Optional.of(snapshot.permitTtl()) : Optional.empty(); }
        catch (RuntimeException invalid) { return Optional.empty(); }
    }
    @Override public boolean allowsIssuanceAt(RecoveryTarget target, Instant issued, Instant expires) {
        try {
            return snapshot.environmentId().equals(target.environmentId()) && snapshot.active(issued)
                    && issued.plus(snapshot.permitTtl()).equals(expires) && !expires.isAfter(snapshot.validUntil());
        } catch (RuntimeException invalid) { return false; }
    }
}
