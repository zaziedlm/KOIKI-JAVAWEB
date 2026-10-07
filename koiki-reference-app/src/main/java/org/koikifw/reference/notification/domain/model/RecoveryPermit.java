package org.koikifw.reference.notification.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;
import org.jspecify.annotations.Nullable;

/** Initial JPA mapping; operational creation and update entrypoints are not yet exposed. */
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

    @Column(name = "closed_at", nullable = true)
    private @Nullable Instant closedAt;

    @Column(name = "confirmed_by", nullable = true)
    private @Nullable UUID confirmedBy;

    @Column(name = "result_ref", nullable = true, length = 255)
    private @Nullable String resultRef;

    @Version
    @Column(name = "version", nullable = true)
    private @Nullable Long version;

    protected RecoveryPermit() {
    }
}
