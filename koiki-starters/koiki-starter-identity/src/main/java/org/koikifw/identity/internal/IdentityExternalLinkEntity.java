package org.koikifw.identity.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "KoikiIdentityExternalLink")
@Table(name = "koiki_external_identity_link")
class IdentityExternalLinkEntity {

    @Id
    @Column(name = "link_id", nullable = false, updatable = false)
    private UUID linkId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "issuer", nullable = false, length = 2048)
    private String issuer;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IdentityExternalLinkEntity() {
        this.linkId = new UUID(0L, 0L);
        this.userId = new UUID(0L, 0L);
        this.issuer = "";
        this.subject = "";
        this.createdAt = Instant.EPOCH;
        this.updatedAt = Instant.EPOCH;
    }

    static IdentityExternalLinkEntity create(
            UUID linkId, UUID userId, String issuer, String subject, Instant now) {
        IdentityExternalLinkEntity link = new IdentityExternalLinkEntity();
        link.linkId = linkId;
        link.userId = userId;
        link.issuer = issuer;
        link.subject = subject;
        link.createdAt = now;
        link.updatedAt = now;
        return link;
    }
}
