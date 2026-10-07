package org.koikifw.reference.notification.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;
import org.jspecify.annotations.Nullable;

/** Permit invariants only; authorization and trustworthy operational proof belong to Application. */
@Entity
@Table(name = "kkref_notification_recovery_permit")
@DynamicUpdate
public class RecoveryPermit {

    @Id
    @Column(name = "permit_id", nullable = false, updatable = false)
    private UUID permitId = new UUID(0L, 0L);

    @Column(name = "environment_id", nullable = false, updatable = false, length = 128)
    private String environmentId = "";

    @Column(name = "publication_id", nullable = false, updatable = false)
    private UUID publicationId = new UUID(0L, 0L);

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId = new UUID(0L, 0L);

    @Column(name = "listener_id", nullable = false, updatable = false, columnDefinition = "text")
    private String listenerId = "";

    @Column(name = "expected_attempt", nullable = false, updatable = false)
    private int expectedAttempt;

    @Column(name = "actor_id", nullable = false, updatable = false)
    private UUID actorId = new UUID(0L, 0L);

    @Column(name = "reason_code", nullable = false, updatable = false, length = 128)
    private String reasonCode = "";

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt = Instant.EPOCH;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt = Instant.EPOCH;

    @Column(name = "closed_at", nullable = true, insertable = false)
    private @Nullable Instant closedAt;

    @Column(name = "confirmed_by", nullable = true, insertable = false)
    private @Nullable UUID confirmedBy;

    @Column(name = "result_ref", nullable = true, length = 255, insertable = false)
    private @Nullable String resultRef;

    @Version
    @Column(name = "version", nullable = true, insertable = false)
    private @Nullable Long version;

    protected RecoveryPermit() {
    }

    /** Times must be supplied by Application from its Clock and approved TTL policy. */
    public static RecoveryPermit issue(
            UUID permitId, String environmentId, UUID publicationId, UUID eventId,
            String listenerId, int expectedAttempt, UUID actorId, String reasonCode,
            Instant issuedAt, Instant expiresAt) {
        RecoveryPermit permit = new RecoveryPermit();
        permit.permitId = Objects.requireNonNull(permitId, "permitId");
        permit.environmentId = requireText(environmentId, "environmentId", 128);
        permit.publicationId = Objects.requireNonNull(publicationId, "publicationId");
        permit.eventId = Objects.requireNonNull(eventId, "eventId");
        permit.listenerId = requireText(listenerId, "listenerId", Integer.MAX_VALUE);
        if (expectedAttempt < 0) {
            throw new IllegalArgumentException("expectedAttempt must be nonnegative");
        }
        permit.expectedAttempt = expectedAttempt;
        permit.actorId = Objects.requireNonNull(actorId, "actorId");
        permit.reasonCode = requireText(reasonCode, "reasonCode", 128);
        permit.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt").truncatedTo(ChronoUnit.MICROS);
        permit.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt").truncatedTo(ChronoUnit.MICROS);
        if (!permit.expiresAt.isAfter(permit.issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt at database precision");
        }
        return permit;
    }

    /** Checks values and expiry without recording consumption or authorizing delivery. */
    public void requireConsumable(
            String environmentId, UUID publicationId, UUID eventId, String listenerId,
            int expectedAttempt, Instant now) {
        Objects.requireNonNull(now, "now");
        if (closedAt != null) {
            throw new IllegalStateException("Permit is closed");
        }
        if (!this.environmentId.equals(environmentId)
                || !this.publicationId.equals(publicationId)
                || !this.eventId.equals(eventId)
                || !this.listenerId.equals(listenerId)
                || this.expectedAttempt != expectedAttempt) {
            throw new IllegalArgumentException("Permit target does not match");
        }
        // Keep the comparison time unrounded: precision adjustment must never extend validity.
        if (!now.isBefore(expiresAt)) {
            throw new IllegalStateException("Permit has expired");
        }
    }

    /** Records a human resolution after Application verifies the actor and operational evidence.
     * Closure may follow expiry and does not assert delivery success.
     */
    public void close(Instant closedAt, UUID confirmedBy, String resultRef) {
        if (this.closedAt != null) {
            throw new IllegalStateException("Permit is already closed");
        }
        Instant time = Objects.requireNonNull(closedAt, "closedAt").truncatedTo(ChronoUnit.MICROS);
        UUID confirmer = Objects.requireNonNull(confirmedBy, "confirmedBy");
        String reference = requireText(resultRef, "resultRef", 255);
        // Validate the whole tuple before changing any persistent field.
        this.closedAt = time;
        this.confirmedBy = confirmer;
        this.resultRef = reference;
    }

    private static String requireText(String value, String name, int maxLength) {
        Objects.requireNonNull(value, name);
        if (value.isBlank() || value.codePointCount(0, value.length()) > maxLength) {
            throw new IllegalArgumentException(name + " must be nonblank and within column length");
        }
        return value;
    }

    public UUID permitId() { return permitId; }
    public String environmentId() { return environmentId; }
    public UUID publicationId() { return publicationId; }
    public UUID eventId() { return eventId; }
    public String listenerId() { return listenerId; }
    public int expectedAttempt() { return expectedAttempt; }
    public UUID actorId() { return actorId; }
    public String reasonCode() { return reasonCode; }
    public Instant issuedAt() { return issuedAt; }
    public Instant expiresAt() { return expiresAt; }
    public @Nullable Instant closedAt() { return closedAt; }
    public @Nullable UUID confirmedBy() { return confirmedBy; }
    public @Nullable String resultRef() { return resultRef; }
    public @Nullable Long version() { return version; }
}
