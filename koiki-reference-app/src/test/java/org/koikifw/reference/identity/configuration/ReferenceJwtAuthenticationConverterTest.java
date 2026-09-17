package org.koikifw.reference.identity.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.koikifw.identity.AuthenticationSource;
import org.koikifw.identity.FrameworkPrincipal;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.identity.IdentityQuery;
import org.koikifw.identity.IdentityUser;
import org.koikifw.identity.UserStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

class ReferenceJwtAuthenticationConverterTest {

    private final IdentityQuery identities = mock(IdentityQuery.class);
    private final ReferenceJwtAuthenticationConverter converter =
            new ReferenceJwtAuthenticationConverter(identities);

    @Test
    void intersectsExactScopeWithCurrentDatabasePermission() {
        FrameworkUserId userId = FrameworkUserId.parse(UUID.randomUUID().toString());
        when(identities.findById(userId)).thenReturn(Optional.of(user(
                userId, UserStatus.ACTIVE, Set.of("EXPENSE:APPLY", "MASTER:ADMIN"))));

        var authentication = converter.convert(jwt(userId.toString(), "expense.apply other"));

        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("EXPENSE:APPLY");
        assertThat(authentication.getPrincipal()).isInstanceOf(FrameworkPrincipal.class);
        FrameworkPrincipal principal =
                (FrameworkPrincipal) Objects.requireNonNull(authentication.getPrincipal());
        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.authenticationSource()).isEqualTo(AuthenticationSource.BEARER);
        assertThat(principal.permissions()).containsExactly("EXPENSE:APPLY");
        assertThat(authentication.getCredentials()).isEqualTo("");
    }

    @Test
    void grantsNothingWhenEitherScopeOrDatabasePermissionIsAbsent() {
        FrameworkUserId noScope = FrameworkUserId.parse(UUID.randomUUID().toString());
        when(identities.findById(noScope)).thenReturn(Optional.of(user(
                noScope, UserStatus.ACTIVE, Set.of("EXPENSE:APPLY"))));
        assertThat(converter.convert(jwt(noScope.toString(), "Expense.Apply"))
                        .getAuthorities())
                .isEmpty();

        FrameworkUserId noPermission = FrameworkUserId.parse(UUID.randomUUID().toString());
        when(identities.findById(noPermission)).thenReturn(Optional.of(user(
                noPermission, UserStatus.ACTIVE, Set.of())));
        assertThat(converter.convert(jwt(noPermission.toString(), "expense.apply"))
                        .getAuthorities())
                .isEmpty();
    }

    @Test
    void rejectsMalformedUnknownAndDisabledUsersAsAuthenticationFailures() {
        assertThatThrownBy(() -> converter.convert(jwt("not-a-uuid", "expense.apply")))
                .isInstanceOf(BadCredentialsException.class);

        FrameworkUserId unknown = FrameworkUserId.parse(UUID.randomUUID().toString());
        when(identities.findById(unknown)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> converter.convert(jwt(unknown.toString(), "expense.apply")))
                .isInstanceOf(BadCredentialsException.class);

        FrameworkUserId disabled = FrameworkUserId.parse(UUID.randomUUID().toString());
        when(identities.findById(disabled)).thenReturn(Optional.of(user(
                disabled, UserStatus.DISABLED, Set.of("EXPENSE:APPLY"))));
        assertThatThrownBy(() -> converter.convert(jwt(disabled.toString(), "expense.apply")))
                .isInstanceOf(BadCredentialsException.class);
    }

    private static IdentityUser user(
            FrameworkUserId userId, UserStatus status, Set<String> permissions) {
        return new IdentityUser(
                userId, "identity-only@example.test", status, Set.of(), permissions, 0);
    }

    private static Jwt jwt(String userId, String scope) {
        Instant now = Instant.now();
        return new Jwt(
                "redacted-fixture-token",
                now.minusSeconds(1),
                now.plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of(
                        "sub", "opaque-subject",
                        "scope", scope,
                        "koiki_user_id", userId));
    }
}
