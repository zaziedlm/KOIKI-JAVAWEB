package org.koikifw.reference.notification.application.port.outbound;

import java.util.UUID;

public interface RecoveryScopePort {
    enum Decision { ALLOWED, OUTSIDE, UNAVAILABLE }
    Decision check(UUID userId, String capability, String environmentId, UUID publicationId);
}
