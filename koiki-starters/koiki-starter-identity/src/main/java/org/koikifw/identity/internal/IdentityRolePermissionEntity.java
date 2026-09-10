package org.koikifw.identity.internal;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@SuppressWarnings("UnusedVariable")
@Entity(name = "KoikiIdentityRolePermission")
@Table(name = "koiki_role_permission")
class IdentityRolePermissionEntity {

    @EmbeddedId
    private RolePermissionId id;

    protected IdentityRolePermissionEntity() {
        this.id = new RolePermissionId();
    }

    IdentityRolePermissionEntity(UUID roleId, UUID permissionId) {
        this.id = new RolePermissionId(roleId, permissionId);
    }
}
