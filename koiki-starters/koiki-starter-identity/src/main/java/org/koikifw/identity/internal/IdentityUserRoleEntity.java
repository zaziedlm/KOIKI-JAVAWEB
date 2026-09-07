package org.koikifw.identity.internal;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@SuppressWarnings("UnusedVariable")
@Entity(name = "KoikiIdentityUserRole")
@Table(name = "koiki_user_role")
class IdentityUserRoleEntity {

    @EmbeddedId
    private UserRoleId id;

    protected IdentityUserRoleEntity() {
        this.id = new UserRoleId();
    }
}
