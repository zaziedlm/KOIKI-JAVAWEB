package org.koikifw.reference.notification.application.query;

import java.util.Objects;
import java.util.UUID;

/** Value snapshot supplied by a trusted current-target adapter, never a publication entity. */
public record RecoveryTarget(String environmentId, UUID publicationId, UUID eventId,
        String listenerId, int expectedAttempt) {
    public RecoveryTarget {
        Objects.requireNonNull(environmentId, "environmentId");
        Objects.requireNonNull(publicationId, "publicationId");
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(listenerId, "listenerId");
        if (environmentId.isBlank() || listenerId.isBlank() || expectedAttempt < 0) {
            throw new IllegalArgumentException("Invalid target values");
        }
    }
}
