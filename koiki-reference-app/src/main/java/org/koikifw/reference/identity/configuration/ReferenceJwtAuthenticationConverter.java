package org.koikifw.reference.identity.configuration;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.identity.IdentityQuery;
import org.koikifw.identity.IdentityUser;
import org.koikifw.identity.UserStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** Intersects the exact token scope with current Framework-owned identity permission. */
final class ReferenceJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    static final String USER_ID_CLAIM = "koiki_user_id";
    static final String APPLY_SCOPE = "expense.apply";
    static final String APPLY_PERMISSION = "EXPENSE:APPLY";

    private final IdentityQuery identities;

    ReferenceJwtAuthenticationConverter(IdentityQuery identities) {
        this.identities = identities;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        FrameworkUserId userId = parseUserId(jwt);
        IdentityUser user = identities.findById(userId)
                .filter(candidate -> candidate.status() == UserStatus.ACTIVE)
                .orElseThrow(() -> new BadCredentialsException("Bearer user is unavailable"));

        boolean allowed = hasExactScope(jwt) && user.permissionCodes().contains(APPLY_PERMISSION);
        Set<String> permissions = allowed ? Set.of(APPLY_PERMISSION) : Set.of();
        List<SimpleGrantedAuthority> authorities = permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new ReferenceBearerAuthenticationToken(
                new ReferenceBearerPrincipal(userId, permissions), authorities);
    }

    private static FrameworkUserId parseUserId(Jwt jwt) {
        String claim = jwt.getClaimAsString(USER_ID_CLAIM);
        try {
            return FrameworkUserId.parse(Objects.requireNonNull(claim, USER_ID_CLAIM));
        } catch (RuntimeException exception) {
            throw new BadCredentialsException("Bearer user claim is invalid", exception);
        }
    }

    private static boolean hasExactScope(Jwt jwt) {
        Object claim = jwt.getClaims().get("scope");
        if (!(claim instanceof String scopes)) {
            return false;
        }
        return scopes.lines()
                .flatMap(line -> java.util.Arrays.stream(line.split(" ")))
                .anyMatch(APPLY_SCOPE::equals);
    }
}
