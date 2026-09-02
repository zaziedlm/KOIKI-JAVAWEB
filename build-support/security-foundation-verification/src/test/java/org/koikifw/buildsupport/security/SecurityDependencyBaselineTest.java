package org.koikifw.buildsupport.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.SecurityFilterChain;

class SecurityDependencyBaselineTest {

    @Test
    void providesBootManagedProductionAndTestSecurityTypes() {
        assertThat(SecurityFilterChain.class).isNotNull();
        assertThat(ClientRegistration.class).isNotNull();
        assertThat(JwtDecoder.class).isNotNull();
        assertThat(JwtAuthenticationToken.class).isNotNull();
        assertThat(SecurityMockMvcRequestPostProcessors.class).isNotNull();
    }
}
