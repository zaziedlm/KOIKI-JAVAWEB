package org.koikifw.reference.identity.configuration;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/** Authentication result that deliberately retains no JWT or claim map. */
final class ReferenceBearerAuthenticationToken extends AbstractAuthenticationToken {

    private final ReferenceBearerPrincipal principal;

    ReferenceBearerAuthenticationToken(
            ReferenceBearerPrincipal principal,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public ReferenceBearerPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public String getName() {
        return principal.userId().toString();
    }
}
