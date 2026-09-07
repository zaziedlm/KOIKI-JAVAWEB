package org.koikifw.identity.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.koikifw.identity.AuthenticationSource;
import org.koikifw.identity.FrameworkPrincipal;
import org.koikifw.identity.FrameworkUserId;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

final class KoikiIdentityUserDetails
        implements UserDetails, CredentialsContainer, FrameworkPrincipal, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final FrameworkUserId userId;
    private final Set<String> permissions;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean authenticationAllowed;
    private final long credentialVersion;
    private transient String password;

    KoikiIdentityUserDetails(
            FrameworkUserId userId,
            String password,
            Set<String> permissions,
            Collection<? extends GrantedAuthority> authorities,
            boolean authenticationAllowed,
            long credentialVersion) {
        this.userId = userId;
        this.password = password;
        this.permissions = Set.copyOf(new LinkedHashSet<>(permissions));
        this.authorities = List.copyOf(authorities);
        this.authenticationAllowed = authenticationAllowed;
        this.credentialVersion = credentialVersion;
    }

    @Override
    public FrameworkUserId userId() {
        return userId;
    }

    @Override
    public AuthenticationSource authenticationSource() {
        return AuthenticationSource.LOCAL;
    }

    @Override
    public Set<String> permissions() {
        return permissions;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return userId.toString();
    }

    @Override
    public void eraseCredentials() {
        password = "";
    }

    boolean authenticationAllowed() {
        return authenticationAllowed;
    }

    long credentialVersion() {
        return credentialVersion;
    }

    KoikiIdentityUserDetails withPassword(String encodedPassword) {
        return new KoikiIdentityUserDetails(
                userId,
                encodedPassword,
                permissions,
                authorities,
                authenticationAllowed,
                credentialVersion + 1);
    }

    @Serial
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        password = "";
    }
}
