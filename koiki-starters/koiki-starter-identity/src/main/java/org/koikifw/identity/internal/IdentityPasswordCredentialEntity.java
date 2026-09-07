package org.koikifw.identity.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Entity(name = "KoikiIdentityPasswordCredential")
@Table(name = "koiki_password_credential")
class IdentityPasswordCredentialEntity {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "encoded_password", nullable = false, length = 512)
    private String encodedPassword;

    @Column(name = "locked_until")
    private @Nullable Instant lockedUntil;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IdentityPasswordCredentialEntity() {
        this.userId = new UUID(0L, 0L);
        this.encodedPassword = "";
        this.createdAt = Instant.EPOCH;
        this.updatedAt = Instant.EPOCH;
    }
}
