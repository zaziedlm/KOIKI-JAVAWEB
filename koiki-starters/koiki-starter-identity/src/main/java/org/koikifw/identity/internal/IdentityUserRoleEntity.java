package org.koikifw.identity.internal;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@SuppressWarnings("UnusedVariable")
@Entity(name = "KoikiIdentityUserRole")
@Table(name = "koiki_user_role")
class IdentityUserRoleEntity {

    @EmbeddedId
    private UserRoleId id;

    protected IdentityUserRoleEntity() {
        this.id = new UserRoleId();
    }

    IdentityUserRoleEntity(UUID userId, UUID roleId) {
        this.id = new UserRoleId(userId, roleId);
    }
}
