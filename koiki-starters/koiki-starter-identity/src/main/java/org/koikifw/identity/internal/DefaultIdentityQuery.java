package org.koikifw.identity.internal;

import jakarta.persistence.EntityManager;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.identity.IdentityQuery;
import org.koikifw.identity.IdentityUser;
import org.koikifw.identity.UserStatus;
import org.springframework.transaction.annotation.Transactional;

public class DefaultIdentityQuery implements IdentityQuery {

    private final EntityManager entityManager;

    DefaultIdentityQuery(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdentityUser> findById(FrameworkUserId userId) {
        IdentityUserEntity user = entityManager.find(IdentityUserEntity.class, userId.value());
        if (user == null) {
            return Optional.empty();
        }

        List<String> roles = entityManager
                .createQuery(
                        """
                        select role.roleCode
                        from KoikiIdentityRole role, KoikiIdentityUserRole userRole
                        where userRole.id.userId = :userId
                          and role.roleId = userRole.id.roleId
                        order by role.roleCode
                        """,
                        String.class)
                .setParameter("userId", user.userId())
                .getResultList();
        List<String> permissions = entityManager
                .createQuery(
                        """
                        select distinct permission.permissionCode
                        from KoikiIdentityPermission permission,
                             KoikiIdentityRolePermission rolePermission,
                             KoikiIdentityUserRole userRole
                        where userRole.id.userId = :userId
                          and rolePermission.id.roleId = userRole.id.roleId
                          and permission.permissionId = rolePermission.id.permissionId
                        order by permission.permissionCode
                        """,
                        String.class)
                .setParameter("userId", user.userId())
                .getResultList();

        return Optional.of(new IdentityUser(
                FrameworkUserId.parse(user.userId().toString()),
                user.email(),
                UserStatus.valueOf(user.status()),
                immutableOrderedSet(roles),
                immutableOrderedSet(permissions),
                user.version()));
    }

    private static Set<String> immutableOrderedSet(List<String> values) {
        return Set.copyOf(new LinkedHashSet<>(values));
    }
}
