package org.koikifw.identity.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
class UserRoleId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    protected UserRoleId() {
        this.userId = new UUID(0L, 0L);
        this.roleId = new UUID(0L, 0L);
    }

    UUID userId() {
        return userId;
    }

    UUID roleId() {
        return roleId;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof UserRoleId otherId
                && userId.equals(otherId.userId)
                && roleId.equals(otherId.roleId));
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roleId);
    }
}
