package org.koikifw.reference.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.identity.IdentityQuery;
import org.koikifw.identity.IdentityUser;
import org.koikifw.identity.UserStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = IdentityUserManagementMethodSecurityTest.TestConfiguration.class)
class IdentityUserManagementMethodSecurityTest {

    private static final FrameworkUserId USER_ID =
            FrameworkUserId.parse(UUID.randomUUID().toString());

    @Autowired
    private IdentityUserManagement management;

    @Autowired
    private IdentityQuery query;

    @BeforeEach
    void resetQuery() {
        reset(query);
    }

    @Test
    @WithMockUser(authorities = "IDENTITY:ADMIN")
    void allowsIdentityAdministratorAtApplicationBoundary() {
        when(query.findById(USER_ID)).thenReturn(Optional.of(new IdentityUser(
                USER_ID,
                "admin-visible@example.test",
                UserStatus.ACTIVE,
                Set.of(),
                Set.of(),
                0)));

        assertThat(management.findUser(USER_ID)).isPresent();
    }

    @Test
    @WithMockUser(authorities = "EXPENSE:READ")
    void rejectsInsufficientPermissionBeforeCallingFrameworkQuery() {
        assertThrows(AccessDeniedException.class, () -> management.findUser(USER_ID));
        verifyNoInteractions(query);
    }

    @Test
    @WithAnonymousUser
    void rejectsAnonymousInvocationBeforeCallingFrameworkQuery() {
        assertThrows(AccessDeniedException.class, () -> management.findUser(USER_ID));
        verifyNoInteractions(query);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestConfiguration {

        @Bean
        IdentityQuery identityQuery() {
            return mock(IdentityQuery.class);
        }

        @Bean
        IdentityUserManagement identityUserManagement(IdentityQuery identityQuery) {
            return new IdentityUserManagement(identityQuery);
        }
    }
}
