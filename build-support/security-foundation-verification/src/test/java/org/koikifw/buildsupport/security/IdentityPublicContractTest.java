package org.koikifw.buildsupport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.identity.IdentityFailure;
import org.koikifw.identity.IdentityOperationException;
import org.koikifw.identity.IdentityUser;
import org.koikifw.identity.UserStatus;

class IdentityPublicContractTest {

    @Test
    void treatsFrameworkUserIdAsAnOpaqueUuidValue() {
        UUID value = UUID.fromString("00000000-0000-4000-8000-000000000201");

        FrameworkUserId userId = FrameworkUserId.parse(value.toString());

        assertThat(userId.value()).isEqualTo(value);
        assertThat(userId).isEqualTo(FrameworkUserId.parse(value.toString()));
        assertThat(userId.toString()).isEqualTo(value.toString());
        assertThatThrownBy(() -> FrameworkUserId.parse("not-a-user-id"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void makesIdentityUserAuthoritySetsImmutable() {
        IdentityUser user = new IdentityUser(
                FrameworkUserId.parse("00000000-0000-4000-8000-000000000201"),
                email("identity"),
                UserStatus.ACTIVE,
                Set.of("OPERATOR"),
                Set.of("ORDER:READ"),
                0L);

        assertThatThrownBy(() -> user.roleCodes().add("ADMIN"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> user.permissionCodes().add("ORDER:WRITE"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void exposesOnlySafeIdentityFailureDetails() {
        IdentityOperationException failure =
                new IdentityOperationException(IdentityFailure.CONFLICT);

        assertThat(failure.failure()).isEqualTo(IdentityFailure.CONFLICT);
        assertThat(failure).hasMessage("Identity operation failed.").hasNoCause();
        assertThat(failure.toString()).doesNotContain("email", "password", "SQL");
    }

    private static String email(String localPart) {
        return localPart + Character.toString(64) + "invalid.example";
    }
}
