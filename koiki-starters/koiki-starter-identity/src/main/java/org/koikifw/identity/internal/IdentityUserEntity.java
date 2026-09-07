package org.koikifw.identity.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "KoikiIdentityUser")
@Table(name = "koiki_user")
class IdentityUserEntity {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "canonical_email", nullable = false, length = 320)
    private String canonicalEmail;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IdentityUserEntity() {
        this.userId = new UUID(0L, 0L);
        this.email = "";
        this.canonicalEmail = "";
        this.status = "ACTIVE";
        this.createdAt = Instant.EPOCH;
        this.updatedAt = Instant.EPOCH;
    }

    static IdentityUserEntity create(UUID userId, IdentityEmail email, Instant now) {
        IdentityUserEntity user = new IdentityUserEntity();
        user.userId = userId;
        user.updateEmailValues(email);
        user.createdAt = now;
        user.updatedAt = now;
        return user;
    }

    void changeEmail(IdentityEmail email, Instant now) {
        updateEmailValues(email);
        updatedAt = now;
    }

    UUID userId() {
        return userId;
    }

    String email() {
        return email;
    }

    String status() {
        return status;
    }

    long version() {
        return version;
    }

    private void updateEmailValues(IdentityEmail email) {
        this.email = email.value();
        this.canonicalEmail = email.canonicalValue();
    }
}
