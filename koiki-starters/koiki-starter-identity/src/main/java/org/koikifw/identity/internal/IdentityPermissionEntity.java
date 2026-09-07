package org.koikifw.identity.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "KoikiIdentityPermission")
@Table(name = "koiki_permission")
class IdentityPermissionEntity {

    @Id
    @Column(name = "permission_id", nullable = false, updatable = false)
    private UUID permissionId;

    @Column(name = "permission_code", nullable = false, length = 100)
    private String permissionCode;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IdentityPermissionEntity() {
        this.permissionId = new UUID(0L, 0L);
        this.permissionCode = "";
        this.createdAt = Instant.EPOCH;
        this.updatedAt = Instant.EPOCH;
    }

    static IdentityPermissionEntity create(UUID permissionId, String permissionCode, Instant now) {
        IdentityPermissionEntity permission = new IdentityPermissionEntity();
        permission.permissionId = permissionId;
        permission.permissionCode = permissionCode;
        permission.createdAt = now;
        permission.updatedAt = now;
        return permission;
    }

    UUID permissionId() {
        return permissionId;
    }

    String permissionCode() {
        return permissionCode;
    }
}
