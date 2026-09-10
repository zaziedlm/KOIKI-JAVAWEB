package org.koikifw.identity.internal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.koikifw.audit.AuditActor;
import org.koikifw.audit.AuditEvent;
import org.koikifw.audit.AuditRecordingException;
import org.koikifw.audit.AuditResult;
import org.koikifw.audit.BusinessAuditRecorder;
import org.koikifw.audit.SecurityAuditRecorder;
import org.koikifw.identity.FrameworkPrincipal;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.identity.IdentityAdministration;
import org.koikifw.identity.IdentityFailure;
import org.koikifw.identity.IdentityOperationException;
import org.koikifw.identity.UserSessionInvalidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/** JPA implementation of the audited Identity administration contract. */
public class DefaultIdentityAdministration implements IdentityAdministration {

    private static final Logger logger = LoggerFactory.getLogger(DefaultIdentityAdministration.class);
    private static final String CODE_PATTERN = "^[A-Z][A-Z0-9_:]{0,99}$";

    private final EntityManager entityManager;
    private final Clock clock;
    private final PasswordEncoder passwordEncoder;
    private final CompromisedPasswordChecker compromisedPasswordChecker;
    private final BusinessAuditRecorder businessAuditRecorder;
    private final SecurityAuditRecorder securityAuditRecorder;
    private final UserSessionInvalidator sessionInvalidator;
    private final IdentityAuthenticationProperties properties;

    DefaultIdentityAdministration(
            EntityManager entityManager,
            Clock clock,
            PasswordEncoder passwordEncoder,
            CompromisedPasswordChecker compromisedPasswordChecker,
            BusinessAuditRecorder businessAuditRecorder,
            SecurityAuditRecorder securityAuditRecorder,
            UserSessionInvalidator sessionInvalidator,
            IdentityAuthenticationProperties properties) {
        this.entityManager = entityManager;
        this.clock = clock;
        this.passwordEncoder = passwordEncoder;
        this.compromisedPasswordChecker = compromisedPasswordChecker;
        this.businessAuditRecorder = businessAuditRecorder;
        this.securityAuditRecorder = securityAuditRecorder;
        this.sessionInvalidator = sessionInvalidator;
        this.properties = properties;
    }

    @Override
    @Transactional
    public FrameworkUserId createUser(String email) {
        IdentityEmail validatedEmail = validEmail(email);
        return execute(() -> {
            AuditActor actor = currentActor();
            if (findUserByCanonicalEmail(validatedEmail.canonicalValue()) != null) {
                throw failure(IdentityFailure.CONFLICT);
            }
            Instant now = clock.instant();
            UUID id = UUID.randomUUID();
            entityManager.persist(IdentityUserEntity.create(id, validatedEmail, now));
            businessAuditRecorder.record(businessEvent(actor, "CREATE_USER", id));
            entityManager.flush();
            return FrameworkUserId.parse(id.toString());
        });
    }

    @Override
    @Transactional
    public void changeEmail(FrameworkUserId userId, String email, long expectedVersion) {
        FrameworkUserId id = requireUserId(userId);
        IdentityEmail validatedEmail = validEmail(email);
        requireExpectedVersion(expectedVersion);
        execute(() -> {
            AuditActor actor = currentActor();
            IdentityUserEntity user = requireUser(id);
            requireVersion(user.version(), expectedVersion);
            IdentityUserEntity collision = findUserByCanonicalEmail(validatedEmail.canonicalValue());
            if (collision != null && !collision.userId().equals(id.value())) {
                throw failure(IdentityFailure.CONFLICT);
            }
            user.changeEmail(validatedEmail, clock.instant());
            businessAuditRecorder.record(businessEvent(actor, "CHANGE_EMAIL", id.value()));
            entityManager.flush();
            return null;
        });
    }

    @Override
    @Transactional
    public void enable(FrameworkUserId userId, long expectedVersion) {
        FrameworkUserId id = requireUserId(userId);
        requireExpectedVersion(expectedVersion);
        execute(() -> {
            AuditActor actor = currentActor();
            IdentityUserEntity user = requireUser(id);
            requireVersion(user.version(), expectedVersion);
            user.enable(clock.instant());
            businessAuditRecorder.record(businessEvent(actor, "ENABLE_ACCOUNT", id.value()));
            entityManager.flush();
            return null;
        });
    }

    @Override
    @Transactional
    public void disable(FrameworkUserId userId, long expectedVersion) {
        FrameworkUserId id = requireUserId(userId);
        requireExpectedVersion(expectedVersion);
        execute(() -> {
            AuditActor actor = currentActor();
            IdentityUserEntity user = requireUser(id);
            requireVersion(user.version(), expectedVersion);
            user.disable(clock.instant());
            entityManager.flush();
            invalidate(id);
            try {
                securityAuditRecorder.record(securityEvent(actor, "DISABLE_ACCOUNT", id.value()));
            } catch (AuditRecordingException exception) {
                logger.error("KOIKI-ID-ADMIN-002: account disable audit failed", exception);
            }
            return null;
        });
    }

    @Override
    @Transactional
    public void setLocalPassword(
            FrameworkUserId userId, char[] rawPassword, long expectedVersion) {
        FrameworkUserId id = requireUserId(userId);
        requireExpectedVersion(expectedVersion);
        if (rawPassword == null) {
            throw failure(IdentityFailure.INVALID_INPUT);
        }
        char[] copy = Arrays.copyOf(rawPassword, rawPassword.length);
        try {
            String normalized = validPassword(copy);
            execute(() -> {
                AuditActor actor = currentActor();
                if (compromisedPasswordChecker.check(normalized).isCompromised()) {
                    throw failure(IdentityFailure.INVALID_INPUT);
                }
                IdentityUserEntity user = requireUser(id);
                requireVersion(user.version(), expectedVersion);
                Instant now = clock.instant();
                String encoded = passwordEncoder.encode(normalized);
                IdentityPasswordCredentialEntity credential = entityManager.find(
                        IdentityPasswordCredentialEntity.class, id.value());
                if (credential == null) {
                    entityManager.persist(
                            IdentityPasswordCredentialEntity.create(id.value(), encoded, now));
                } else {
                    credential.changePassword(encoded, now);
                }
                user.touch(now);
                businessAuditRecorder.record(businessEvent(actor, "SET_LOCAL_PASSWORD", id.value()));
                entityManager.flush();
                invalidate(id);
                return null;
            });
        } finally {
            Arrays.fill(copy, '\0');
        }
    }

    @Override
    @Transactional
    public void unlockLocalCredential(FrameworkUserId userId) {
        FrameworkUserId id = requireUserId(userId);
        execute(() -> {
            AuditActor actor = currentActor();
            requireUser(id);
            IdentityPasswordCredentialEntity credential = entityManager.find(
                    IdentityPasswordCredentialEntity.class, id.value());
            if (credential == null) {
                throw failure(IdentityFailure.NOT_FOUND);
            }
            credential.unlock(clock.instant());
            entityManager
                    .createQuery(
                            "delete from KoikiIdentityLoginAttempt attempt "
                                    + "where attempt.scope = 'ACCOUNT' and attempt.userId = :userId")
                    .setParameter("userId", id.value())
                    .executeUpdate();
            entityManager.flush();
            securityAuditRecorder.record(securityEvent(actor, "UNLOCK_LOCAL_CREDENTIAL", id.value()));
            return null;
        });
    }

    @Override
    @Transactional
    public void createRole(String roleCode) {
        String code = validCode(roleCode);
        execute(() -> {
            AuditActor actor = currentActor();
            if (findRole(code) != null) {
                throw failure(IdentityFailure.CONFLICT);
            }
            IdentityRoleEntity role = IdentityRoleEntity.create(UUID.randomUUID(), code, clock.instant());
            entityManager.persist(role);
            businessAuditRecorder.record(resourceEvent(actor, "CREATE_ROLE", "ROLE", code));
            entityManager.flush();
            return null;
        });
    }

    @Override
    @Transactional
    public void deleteRole(String roleCode, long expectedRoleVersion) {
        String code = validCode(roleCode);
        requireExpectedVersion(expectedRoleVersion);
        execute(() -> {
            AuditActor actor = currentActor();
            IdentityRoleEntity role = requireRole(code);
            requireVersion(role.version(), expectedRoleVersion);
            if (countUserRoles(role.roleId()) != 0 || countRolePermissions(role.roleId()) != 0) {
                throw failure(IdentityFailure.CONFLICT);
            }
            entityManager.remove(role);
            businessAuditRecorder.record(resourceEvent(actor, "DELETE_ROLE", "ROLE", code));
            entityManager.flush();
            return null;
        });
    }

    @Override
    @Transactional
    public void registerPermission(String permissionCode) {
        String code = validCode(permissionCode);
        execute(() -> {
            AuditActor actor = currentActor();
            if (findPermission(code) != null) {
                throw failure(IdentityFailure.CONFLICT);
            }
            entityManager.persist(
                    IdentityPermissionEntity.create(UUID.randomUUID(), code, clock.instant()));
            businessAuditRecorder.record(
                    resourceEvent(actor, "REGISTER_PERMISSION", "PERMISSION", code));
            entityManager.flush();
            return null;
        });
    }

    @Override
    @Transactional
    public void deletePermission(String permissionCode) {
        String code = validCode(permissionCode);
        execute(() -> {
            AuditActor actor = currentActor();
            IdentityPermissionEntity permission = requirePermission(code);
            if (countPermissionRoles(permission.permissionId()) != 0) {
                throw failure(IdentityFailure.CONFLICT);
            }
            entityManager.remove(permission);
            businessAuditRecorder.record(
                    resourceEvent(actor, "DELETE_PERMISSION", "PERMISSION", code));
            entityManager.flush();
            return null;
        });
    }

    @Override
    @Transactional
    public void assignRole(FrameworkUserId userId, String roleCode, long expectedVersion) {
        changeUserRole(userId, roleCode, expectedVersion, true);
    }

    @Override
    @Transactional
    public void revokeRole(FrameworkUserId userId, String roleCode, long expectedVersion) {
        changeUserRole(userId, roleCode, expectedVersion, false);
    }

    @Override
    @Transactional
    public void grantPermission(
            String roleCode, String permissionCode, long expectedRoleVersion) {
        changeRolePermission(roleCode, permissionCode, expectedRoleVersion, true);
    }

    @Override
    @Transactional
    public void revokePermission(
            String roleCode, String permissionCode, long expectedRoleVersion) {
        changeRolePermission(roleCode, permissionCode, expectedRoleVersion, false);
    }

    @Override
    @Transactional
    public void linkExternalIdentity(
            FrameworkUserId userId, String issuer, String subject, long expectedVersion) {
        FrameworkUserId id = requireUserId(userId);
        String exactIssuer = validOpaque(issuer, 2048);
        String exactSubject = validOpaque(subject, 255);
        requireExpectedVersion(expectedVersion);
        execute(() -> {
            AuditActor actor = currentActor();
            IdentityUserEntity user = requireActiveUser(id);
            requireVersion(user.version(), expectedVersion);
            if (findExternalLink(exactIssuer, exactSubject) != null
                    || findUserIssuerLink(id.value(), exactIssuer) != null) {
                throw failure(IdentityFailure.CONFLICT);
            }
            entityManager.persist(IdentityExternalLinkEntity.create(
                    UUID.randomUUID(), id.value(), exactIssuer, exactSubject, clock.instant()));
            user.touch(clock.instant());
            businessAuditRecorder.record(businessEvent(actor, "LINK_EXTERNAL_IDENTITY", id.value()));
            entityManager.flush();
            return null;
        });
    }

    @Override
    @Transactional
    public void unlinkExternalIdentity(
            FrameworkUserId userId, String issuer, long expectedVersion) {
        FrameworkUserId id = requireUserId(userId);
        String exactIssuer = validOpaque(issuer, 2048);
        requireExpectedVersion(expectedVersion);
        execute(() -> {
            AuditActor actor = currentActor();
            IdentityUserEntity user = requireUser(id);
            requireVersion(user.version(), expectedVersion);
            IdentityExternalLinkEntity link = findUserIssuerLink(id.value(), exactIssuer);
            if (link == null) {
                throw failure(IdentityFailure.NOT_FOUND);
            }
            entityManager.remove(link);
            user.touch(clock.instant());
            businessAuditRecorder.record(businessEvent(actor, "UNLINK_EXTERNAL_IDENTITY", id.value()));
            entityManager.flush();
            invalidate(id);
            return null;
        });
    }

    private void changeUserRole(
            FrameworkUserId userId, String roleCode, long expectedVersion, boolean assign) {
        FrameworkUserId id = requireUserId(userId);
        String code = validCode(roleCode);
        requireExpectedVersion(expectedVersion);
        execute(() -> {
            AuditActor actor = currentActor();
            IdentityUserEntity user = requireUser(id);
            requireVersion(user.version(), expectedVersion);
            IdentityRoleEntity role = requireRole(code);
            UserRoleId relationId = new UserRoleId(id.value(), role.roleId());
            IdentityUserRoleEntity relation = entityManager.find(IdentityUserRoleEntity.class, relationId);
            if (assign) {
                if (relation != null) {
                    throw failure(IdentityFailure.CONFLICT);
                }
                entityManager.persist(new IdentityUserRoleEntity(id.value(), role.roleId()));
            } else {
                if (relation == null) {
                    throw failure(IdentityFailure.NOT_FOUND);
                }
                entityManager.remove(relation);
            }
            user.touch(clock.instant());
            businessAuditRecorder.record(
                    businessEvent(actor, assign ? "ASSIGN_ROLE" : "REVOKE_ROLE", id.value())
                            .withResource("ROLE", code));
            entityManager.flush();
            invalidate(id);
            return null;
        });
    }

    private void changeRolePermission(
            String roleCode,
            String permissionCode,
            long expectedRoleVersion,
            boolean grant) {
        String roleValue = validCode(roleCode);
        String permissionValue = validCode(permissionCode);
        requireExpectedVersion(expectedRoleVersion);
        execute(() -> {
            AuditActor actor = currentActor();
            IdentityRoleEntity role = requireRole(roleValue);
            requireVersion(role.version(), expectedRoleVersion);
            IdentityPermissionEntity permission = requirePermission(permissionValue);
            RolePermissionId relationId =
                    new RolePermissionId(role.roleId(), permission.permissionId());
            IdentityRolePermissionEntity relation =
                    entityManager.find(IdentityRolePermissionEntity.class, relationId);
            if (grant) {
                if (relation != null) {
                    throw failure(IdentityFailure.CONFLICT);
                }
                entityManager.persist(new IdentityRolePermissionEntity(
                        role.roleId(), permission.permissionId()));
            } else {
                if (relation == null) {
                    throw failure(IdentityFailure.NOT_FOUND);
                }
                entityManager.remove(relation);
            }
            role.touch(clock.instant());
            businessAuditRecorder.record(resourceEvent(
                    actor,
                    grant ? "GRANT_PERMISSION" : "REVOKE_PERMISSION",
                    "ROLE_PERMISSION",
                    roleValue + "|" + permissionValue));
            entityManager.flush();
            for (UUID affectedUser : userIdsForRole(role.roleId())) {
                invalidate(FrameworkUserId.parse(affectedUser.toString()));
            }
            return null;
        });
    }

    private IdentityUserEntity requireUser(FrameworkUserId userId) {
        IdentityUserEntity user = entityManager.find(IdentityUserEntity.class, userId.value());
        if (user == null) {
            throw failure(IdentityFailure.NOT_FOUND);
        }
        return user;
    }

    private IdentityUserEntity requireActiveUser(FrameworkUserId userId) {
        IdentityUserEntity user = requireUser(userId);
        if (!"ACTIVE".equals(user.status())) {
            throw failure(IdentityFailure.CONFLICT);
        }
        return user;
    }

    private IdentityRoleEntity requireRole(String roleCode) {
        IdentityRoleEntity role = findRole(roleCode);
        if (role == null) {
            throw failure(IdentityFailure.NOT_FOUND);
        }
        return role;
    }

    private IdentityPermissionEntity requirePermission(String permissionCode) {
        IdentityPermissionEntity permission = findPermission(permissionCode);
        if (permission == null) {
            throw failure(IdentityFailure.NOT_FOUND);
        }
        return permission;
    }

    private @Nullable IdentityUserEntity findUserByCanonicalEmail(String canonicalEmail) {
        return entityManager
                .createQuery(
                        "select user from KoikiIdentityUser user "
                                + "where user.canonicalEmail = :canonicalEmail",
                        IdentityUserEntity.class)
                .setParameter("canonicalEmail", canonicalEmail)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    private @Nullable IdentityRoleEntity findRole(String roleCode) {
        return entityManager
                .createQuery(
                        "select role from KoikiIdentityRole role where role.roleCode = :roleCode",
                        IdentityRoleEntity.class)
                .setParameter("roleCode", roleCode)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    private @Nullable IdentityPermissionEntity findPermission(String permissionCode) {
        return entityManager
                .createQuery(
                        "select permission from KoikiIdentityPermission permission "
                                + "where permission.permissionCode = :permissionCode",
                        IdentityPermissionEntity.class)
                .setParameter("permissionCode", permissionCode)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    private @Nullable IdentityExternalLinkEntity findExternalLink(String issuer, String subject) {
        return entityManager
                .createQuery(
                        "select link from KoikiIdentityExternalLink link "
                                + "where link.issuer = :issuer and link.subject = :subject",
                        IdentityExternalLinkEntity.class)
                .setParameter("issuer", issuer)
                .setParameter("subject", subject)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    private @Nullable IdentityExternalLinkEntity findUserIssuerLink(UUID userId, String issuer) {
        return entityManager
                .createQuery(
                        "select link from KoikiIdentityExternalLink link "
                                + "where link.userId = :userId and link.issuer = :issuer",
                        IdentityExternalLinkEntity.class)
                .setParameter("userId", userId)
                .setParameter("issuer", issuer)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    private long countUserRoles(UUID roleId) {
        return entityManager
                .createQuery(
                        "select count(userRole) from KoikiIdentityUserRole userRole "
                                + "where userRole.id.roleId = :roleId",
                        Long.class)
                .setParameter("roleId", roleId)
                .getSingleResult();
    }

    private long countRolePermissions(UUID roleId) {
        return entityManager
                .createQuery(
                        "select count(rolePermission) from KoikiIdentityRolePermission rolePermission "
                                + "where rolePermission.id.roleId = :roleId",
                        Long.class)
                .setParameter("roleId", roleId)
                .getSingleResult();
    }

    private long countPermissionRoles(UUID permissionId) {
        return entityManager
                .createQuery(
                        "select count(rolePermission) from KoikiIdentityRolePermission rolePermission "
                                + "where rolePermission.id.permissionId = :permissionId",
                        Long.class)
                .setParameter("permissionId", permissionId)
                .getSingleResult();
    }

    private List<UUID> userIdsForRole(UUID roleId) {
        return entityManager
                .createQuery(
                        "select userRole.id.userId from KoikiIdentityUserRole userRole "
                                + "where userRole.id.roleId = :roleId",
                        UUID.class)
                .setParameter("roleId", roleId)
                .getResultList();
    }

    private void invalidate(FrameworkUserId userId) {
        try {
            sessionInvalidator.invalidateAll(userId);
        } catch (RuntimeException exception) {
            logger.error("KOIKI-ID-ADMIN-003: session invalidation failed", exception);
            throw failure(IdentityFailure.DEPENDENCY_FAILURE);
        }
    }

    private static AuditActor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof FrameworkPrincipal principal)) {
            throw failure(IdentityFailure.DEPENDENCY_FAILURE);
        }
        return AuditActor.user(principal.userId().toString());
    }

    private static AuditEvent businessEvent(AuditActor actor, String action, UUID subjectId) {
        return AuditEvent.of("IDENTITY_ADMINISTRATION", actor, action, AuditResult.SUCCESS)
                .withSubject(subjectId.toString());
    }

    private static AuditEvent resourceEvent(
            AuditActor actor, String action, String resourceType, String resourceId) {
        return AuditEvent.of("IDENTITY_ADMINISTRATION", actor, action, AuditResult.SUCCESS)
                .withResource(resourceType, resourceId);
    }

    private static AuditEvent securityEvent(AuditActor actor, String action, UUID subjectId) {
        return AuditEvent.of("IDENTITY_PROTECTION", actor, action, AuditResult.SUCCESS)
                .withSubject(subjectId.toString());
    }

    private String validPassword(char[] password) {
        String normalized = Normalizer.normalize(new String(password), Normalizer.Form.NFC);
        int length = normalized.codePointCount(0, normalized.length());
        if (length < 15 || length > properties.getPassword().getMaximumLength()) {
            throw failure(IdentityFailure.INVALID_INPUT);
        }
        return normalized;
    }

    private static FrameworkUserId requireUserId(FrameworkUserId userId) {
        if (userId == null) {
            throw failure(IdentityFailure.INVALID_INPUT);
        }
        return userId;
    }

    private static IdentityEmail validEmail(String email) {
        try {
            return IdentityEmail.from(email);
        } catch (RuntimeException exception) {
            throw failure(IdentityFailure.INVALID_INPUT);
        }
    }

    private static String validCode(String value) {
        if (value == null || !value.matches(CODE_PATTERN)) {
            throw failure(IdentityFailure.INVALID_INPUT);
        }
        return value;
    }

    private static String validOpaque(String value, int maximumLength) {
        if (value == null
                || value.isBlank()
                || value.length() > maximumLength
                || !value.equals(value.strip())
                || value.codePoints().anyMatch(Character::isISOControl)) {
            throw failure(IdentityFailure.INVALID_INPUT);
        }
        return value;
    }

    private static void requireExpectedVersion(long version) {
        if (version < 0) {
            throw failure(IdentityFailure.INVALID_INPUT);
        }
    }

    private static void requireVersion(long actual, long expected) {
        if (actual != expected) {
            throw failure(IdentityFailure.CONCURRENT_MODIFICATION);
        }
    }

    private static IdentityOperationException failure(IdentityFailure failure) {
        return new IdentityOperationException(failure);
    }

    private <T> T execute(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (IdentityOperationException exception) {
            throw exception;
        } catch (OptimisticLockException exception) {
            throw failure(IdentityFailure.CONCURRENT_MODIFICATION);
        } catch (PersistenceException exception) {
            logger.error("KOIKI-ID-ADMIN-004: identity persistence failed", exception);
            throw failure(isConstraintFailure(exception)
                    ? IdentityFailure.CONFLICT
                    : IdentityFailure.DEPENDENCY_FAILURE);
        } catch (RuntimeException exception) {
            logger.error("KOIKI-ID-ADMIN-005: identity dependency failed", exception);
            throw failure(IdentityFailure.DEPENDENCY_FAILURE);
        }
    }

    private static boolean isConstraintFailure(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current.getClass().getSimpleName().equals("ConstraintViolationException")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
