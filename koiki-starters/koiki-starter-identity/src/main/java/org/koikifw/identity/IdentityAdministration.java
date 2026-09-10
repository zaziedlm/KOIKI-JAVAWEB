package org.koikifw.identity;

/** Mutates framework-owned Identity state through audited use cases. */
public interface IdentityAdministration {

    FrameworkUserId createUser(String email);

    void changeEmail(FrameworkUserId userId, String email, long expectedVersion);

    void enable(FrameworkUserId userId, long expectedVersion);

    void disable(FrameworkUserId userId, long expectedVersion);

    /**
     * Sets a local credential. The caller must clear its array after this call; the implementation
     * clears only its defensive copy.
     */
    void setLocalPassword(FrameworkUserId userId, char[] rawPassword, long expectedVersion);

    void unlockLocalCredential(FrameworkUserId userId);

    void createRole(String roleCode);

    void deleteRole(String roleCode, long expectedRoleVersion);

    void registerPermission(String permissionCode);

    void deletePermission(String permissionCode);

    void assignRole(FrameworkUserId userId, String roleCode, long expectedVersion);

    void revokeRole(FrameworkUserId userId, String roleCode, long expectedVersion);

    void grantPermission(String roleCode, String permissionCode, long expectedRoleVersion);

    void revokePermission(String roleCode, String permissionCode, long expectedRoleVersion);

    void linkExternalIdentity(
            FrameworkUserId userId, String issuer, String subject, long expectedVersion);

    void unlinkExternalIdentity(FrameworkUserId userId, String issuer, long expectedVersion);
}
