package org.koikifw.reference.notification.adapter.outbound.configuration;

import java.time.Clock;
import java.util.UUID;
import org.koikifw.reference.notification.application.port.outbound.RecoveryScopePort;

public final class ManagedRecoveryScopeAdapter implements RecoveryScopePort {
    private final ManagedRecoverySnapshot snapshot;
    private final Clock clock;
    public ManagedRecoveryScopeAdapter(ManagedRecoverySnapshot snapshot, Clock clock) { this.snapshot = snapshot; this.clock = clock; }
    @Override public Decision check(UUID user, String capability, String environment, UUID publication) {
        if (!snapshot.active(clock.instant())) return Decision.UNAVAILABLE;
        if (!snapshot.environmentId().equals(environment) || !ManagedRecoverySnapshot.CAPABILITIES.contains(capability)) return Decision.OUTSIDE;
        return snapshot.grants().contains(new ManagedRecoverySnapshot.Grant(user, capability, publication)) ? Decision.ALLOWED : Decision.OUTSIDE;
    }
}
