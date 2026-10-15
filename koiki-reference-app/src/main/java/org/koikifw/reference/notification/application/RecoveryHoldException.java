package org.koikifw.reference.notification.application;

import java.time.Instant;
import java.util.UUID;

/** A selected recovery that must stop before resubmission. */
public final class RecoveryHoldException extends RuntimeException {
    private final String reason;
    private final UUID publicationId;
    private final UUID eventId;
    private final String keyHash;
    private final Instant observedAt;

    public RecoveryHoldException(String reason, UUID publicationId, UUID eventId, String keyHash) {
        super("Recovery HOLD: " + reason);
        this.reason = reason;
        this.publicationId = publicationId;
        this.eventId = eventId;
        this.keyHash = keyHash;
        this.observedAt = Instant.now();
    }

    public String evidenceLine() {
        return "R3B_HOLD reason=" + reason + " publication=" + publicationId + " event=" + eventId
                + " keyHash=" + keyHash + " observedAt=" + observedAt
                + " next=inspect-provider-ledger-and-reference-publication";
    }
}
