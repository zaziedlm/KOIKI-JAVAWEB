package org.koikifw.identity.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
class RolePermissionId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "permission_id", nullable = false)
    private UUID permissionId;

    protected RolePermissionId() {
        this.roleId = new UUID(0L, 0L);
        this.permissionId = new UUID(0L, 0L);
    }

    UUID roleId() {
        return roleId;
    }

    UUID permissionId() {
        return permissionId;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof RolePermissionId otherId
                && roleId.equals(otherId.roleId)
                && permissionId.equals(otherId.permissionId));
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId, permissionId);
    }
}
