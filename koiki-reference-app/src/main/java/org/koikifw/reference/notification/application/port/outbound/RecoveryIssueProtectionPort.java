package org.koikifw.reference.notification.application.port.outbound;

import org.koikifw.reference.notification.application.query.RecoveryTarget;

/** Reference-only admission to an already frozen supplier. Does not authorize delivery. */
public interface RecoveryIssueProtectionPort {
    Scope open(RecoveryTarget target);
    interface Scope extends AutoCloseable {
        RecoveryTarget target();
        boolean active();
        @Override void close();
    }
}
