package org.koikifw.reference.notification.adapter.outbound.configuration;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** A bounded, immutable startup assignment. It does not attest operational evidence or identity. */
public record ManagedRecoverySnapshot(String revision, String sha256, String environmentId,
        Instant validFrom, Instant validUntil, Duration permitTtl, List<Grant> grants) {
    public static final Duration MAX_TTL = Duration.ofMinutes(10);
    public static final Duration MAX_WINDOW = Duration.ofMinutes(30);
    public static final Set<String> CAPABILITIES = Set.of("NOTIFICATION:PERMIT:ISSUE", "NOTIFICATION:PERMIT:READ",
            "NOTIFICATION:PERMIT:EXECUTE", "NOTIFICATION:PERMIT:CLOSE");

    public ManagedRecoverySnapshot {
        requireText(revision); requireText(environmentId);
        Objects.requireNonNull(sha256); Objects.requireNonNull(validFrom); Objects.requireNonNull(validUntil);
        Objects.requireNonNull(permitTtl);
        grants = List.copyOf(grants);
        if (!sha256.matches("[a-f0-9]{64}") || !validFrom.isBefore(validUntil)
                || Duration.between(validFrom, validUntil).compareTo(MAX_WINDOW) > 0
                || permitTtl.isNegative() || permitTtl.isZero() || permitTtl.compareTo(MAX_TTL) > 0
                || grants.size() > 64 || Set.copyOf(grants).size() != grants.size()) {
            throw new IllegalArgumentException("Invalid managed recovery snapshot");
        }
    }

    public boolean active(Instant now) { return !now.isBefore(validFrom) && now.isBefore(validUntil); }

    public record Grant(UUID userId, String capability, UUID publicationId) {
        public Grant {
            Objects.requireNonNull(userId); Objects.requireNonNull(publicationId);
            if (!CAPABILITIES.contains(capability)) throw new IllegalArgumentException("Invalid managed capability");
        }
    }

    private static void requireText(String value) {
        if (value == null || value.isBlank() || value.length() > 128 || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid managed reference");
        }
    }
}
