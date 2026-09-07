package org.koikifw.identity.internal;

import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.koikifw.identity.FrameworkUserId;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

public class IdentityUserDetailsService implements UserDetailsService, UserDetailsPasswordService {

    private final EntityManager entityManager;
    private final Clock clock;

    IdentityUserDetailsService(EntityManager entityManager, Clock clock) {
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        IdentityUserEntity user = findUser(username)
                .orElseThrow(() -> new UsernameNotFoundException("Identity authentication failed."));
        IdentityPasswordCredentialEntity credential = entityManager.find(
                IdentityPasswordCredentialEntity.class, user.userId());
        if (credential == null) {
            throw new UsernameNotFoundException("Identity authentication failed.");
        }

        Set<String> permissions = loadPermissions(user.userId());
        Instant now = clock.instant();
        boolean allowed = "ACTIVE".equals(user.status())
                && (credential.lockedUntil() == null || !credential.lockedUntil().isAfter(now));
        List<SimpleGrantedAuthority> authorities = permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new KoikiIdentityUserDetails(
                FrameworkUserId.parse(user.userId().toString()),
                credential.encodedPassword(),
                permissions,
                authorities,
                allowed,
                credential.version());
    }

    @Override
    @Transactional
    public UserDetails updatePassword(UserDetails user, @Nullable String newPassword) {
        if (!(user instanceof KoikiIdentityUserDetails identityUser) || newPassword == null) {
            throw new UsernameNotFoundException("Identity authentication failed.");
        }
        int updated = entityManager
                .createQuery(
                        """
                        update KoikiIdentityPasswordCredential credential
                        set credential.encodedPassword = :encodedPassword,
                            credential.version = credential.version + 1,
                            credential.updatedAt = :updatedAt
                        where credential.userId = :userId
                          and credential.version = :version
                        """)
                .setParameter("encodedPassword", newPassword)
                .setParameter("updatedAt", clock.instant())
                .setParameter("userId", identityUser.userId().value())
                .setParameter("version", identityUser.credentialVersion())
                .executeUpdate();
        if (updated != 1) {
            throw new UsernameNotFoundException("Identity authentication failed.");
        }
        return identityUser.withPassword(newPassword);
    }

    @Transactional(readOnly = true)
    public Optional<AccountProtectionState> protectionState(String username) {
        return findUser(username).map(user -> {
            IdentityPasswordCredentialEntity credential = entityManager.find(
                    IdentityPasswordCredentialEntity.class, user.userId());
            Instant lockedUntil = credential == null ? null : credential.lockedUntil();
            boolean eligible = "ACTIVE".equals(user.status())
                    && credential != null
                    && (lockedUntil == null || !lockedUntil.isAfter(clock.instant()));
            return new AccountProtectionState(user.userId(), eligible);
        });
    }

    private Optional<IdentityUserEntity> findUser(String username) {
        final String canonicalEmail;
        try {
            canonicalEmail = IdentityEmail.from(username).canonicalValue();
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
        return entityManager
                .createQuery(
                        """
                        select user from KoikiIdentityUser user
                        where user.canonicalEmail = :canonicalEmail
                        """,
                        IdentityUserEntity.class)
                .setParameter("canonicalEmail", canonicalEmail)
                .getResultStream()
                .findFirst();
    }

    private Set<String> loadPermissions(UUID userId) {
        List<String> values = entityManager
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
                .setParameter("userId", userId)
                .getResultList();
        return Set.copyOf(new LinkedHashSet<>(values));
    }
}
