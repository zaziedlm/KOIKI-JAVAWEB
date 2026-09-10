package org.koikifw.identity.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "KoikiIdentityRole")
@Table(name = "koiki_role")
class IdentityRoleEntity {

    @Id
    @Column(name = "role_id", nullable = false, updatable = false)
    private UUID roleId;

    @Column(name = "role_code", nullable = false, length = 100)
    private String roleCode;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IdentityRoleEntity() {
        this.roleId = new UUID(0L, 0L);
        this.roleCode = "";
        this.createdAt = Instant.EPOCH;
        this.updatedAt = Instant.EPOCH;
    }

    static IdentityRoleEntity create(UUID roleId, String roleCode, Instant now) {
        IdentityRoleEntity role = new IdentityRoleEntity();
        role.roleId = roleId;
        role.roleCode = roleCode;
        role.createdAt = now;
        role.updatedAt = now;
        return role;
    }

    UUID roleId() {
        return roleId;
    }

    String roleCode() {
        return roleCode;
    }

    long version() {
        return version;
    }

    void touch(Instant now) {
        updatedAt = now;
    }
}
