package org.koikifw.identity.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Entity(name = "KoikiIdentityLoginAttempt")
@Table(name = "koiki_login_attempt")
class IdentityLoginAttemptEntity {

    @Id
    @Column(name = "attempt_id", nullable = false, updatable = false)
    private UUID attemptId;

    @Column(name = "scope", nullable = false, length = 16)
    private String scope;

    @Column(name = "user_id")
    private @Nullable UUID userId;

    @Column(name = "source_key_id", length = 100)
    private @Nullable String sourceKeyId;

    @Column(name = "source_fingerprint")
    private byte @Nullable [] sourceFingerprint;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    @Column(name = "window_started_at", nullable = false)
    private Instant windowStartedAt;

    @Column(name = "last_failed_at", nullable = false)
    private Instant lastFailedAt;

    @Column(name = "blocked_until")
    private @Nullable Instant blockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IdentityLoginAttemptEntity() {
        this.attemptId = new UUID(0L, 0L);
        this.scope = "ACCOUNT";
        this.failureCount = 0;
        this.windowStartedAt = Instant.EPOCH;
        this.lastFailedAt = Instant.EPOCH;
        this.createdAt = Instant.EPOCH;
        this.updatedAt = Instant.EPOCH;
    }
}
