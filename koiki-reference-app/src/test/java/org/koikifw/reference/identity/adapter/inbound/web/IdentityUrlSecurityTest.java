package org.koikifw.reference.identity.adapter.inbound.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.koikifw.reference.identity.application.IdentityUserManagement;
import org.koikifw.reference.identity.configuration.ReferenceSecurityConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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
    void redirectsSuccessfulLoginToIdentityLookupInsteadOfAnUnrelatedSavedRequest()
            throws Exception {
        mockMvc.perform(formLogin()
                        .user("administrator")
                        .password("test-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/identity/users"));
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
