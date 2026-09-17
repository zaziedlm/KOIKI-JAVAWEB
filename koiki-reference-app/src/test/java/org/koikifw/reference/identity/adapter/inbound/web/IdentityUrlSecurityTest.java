package org.koikifw.reference.identity.adapter.inbound.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.identity.UserStatus;
import org.koikifw.reference.identity.application.IdentityUserManagement;
import org.koikifw.reference.identity.application.IdentityUserView;
import org.koikifw.reference.identity.configuration.ReferenceSecurityConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.authentication.password.CompromisedPasswordDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IdentityManagementController.class)
@Import({ReferenceSecurityConfiguration.class, IdentityUrlSecurityTest.SessionTestConfiguration.class})
class IdentityUrlSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IdentityUserManagement management;

    @MockitoBean(name = "referenceCompromisedPasswordChecker")
    private CompromisedPasswordChecker compromisedPasswordChecker;

    @Test
    void redirectsUnauthenticatedIdentityRequestToLogin() throws Exception {
        mockMvc.perform(get("/identity/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void rejectsAuthenticatedUserWithoutIdentityAdminPermission() throws Exception {
        mockMvc.perform(get("/identity/users").with(user("reader").authorities(() -> "EXPENSE:READ")))
                .andExpect(status().isForbidden());
    }

    @Test
    void permitsIdentityAdministratorAtUrlBoundary() throws Exception {
        mockMvc.perform(get("/identity/users").with(user("administrator").authorities(() -> "IDENTITY:ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void redirectsSuccessfulLoginToReferenceHomeInsteadOfAnUnrelatedSavedRequest()
            throws Exception {
        when(compromisedPasswordChecker.check("test-password"))
                .thenReturn(new CompromisedPasswordDecision(false));

        mockMvc.perform(formLogin()
                        .user("administrator")
                        .password("test-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void rejectsIdentityAdministratorRoleMutationWithoutCsrf() throws Exception {
        mockMvc.perform(post("/identity/users/{userId}/roles", java.util.UUID.randomUUID())
                        .with(user("administrator").authorities(() -> "IDENTITY:ADMIN"))
                        .param("roleCode", "EXPENSE_READER")
                        .param("expectedVersion", "0"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(management);
    }

    @Test
    void rejectsDirectRoleMutationWithoutIdentityAdminPermission() throws Exception {
        mockMvc.perform(post("/identity/users/{userId}/roles", java.util.UUID.randomUUID())
                        .with(user("reader").authorities(() -> "EXPENSE:READ"))
                        .with(csrf())
                        .param("roleCode", "EXPENSE_READER")
                        .param("expectedVersion", "0"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(management);
    }

    @Test
    void permitsIdentityAdministratorRoleMutationWithCsrf() throws Exception {
        java.util.UUID userId = java.util.UUID.randomUUID();

        mockMvc.perform(post("/identity/users/{userId}/roles", userId)
                        .with(user("administrator").authorities(() -> "IDENTITY:ADMIN"))
                        .with(csrf())
                        .param("roleCode", "EXPENSE_READER")
                        .param("expectedVersion", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/identity/users/" + userId));

        verify(management).assignRole(
                org.koikifw.identity.FrameworkUserId.parse(userId.toString()),
                "EXPENSE_READER",
                0);
    }

    @Test
    void rendersRoleMutationFormsWithCsrfAndExpectedVersion() throws Exception {
        FrameworkUserId userId = FrameworkUserId.parse(java.util.UUID.randomUUID().toString());
        when(management.findUser(userId)).thenReturn(Optional.of(new IdentityUserView(
                userId.toString(),
                "admin-visible@example.test",
                UserStatus.ACTIVE,
                List.of("EXPENSE_READER"),
                List.of("EXPENSE:READ"),
                4)));

        mockMvc.perform(get("/identity/users/{userId}", userId)
                        .with(user("administrator").authorities(() -> "IDENTITY:ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("name=\"expectedVersion\" value=\"4\"")))
                .andExpect(content().string(containsString("name=\"roleCode\"")))
                .andExpect(content().string(containsString("Revoke role")));
    }

    static class SessionTestConfiguration {

        @Bean
        Customizer<HttpSecurity> koikiSessionLogoutCustomizer() {
            return http -> { };
        }

        @Bean
        UserDetailsService testUserDetailsService() {
            return new InMemoryUserDetailsManager(User.withUsername("administrator")
                    .password("{noop}test-password")
                    .authorities("IDENTITY:ADMIN")
                    .build());
        }
    }
}
