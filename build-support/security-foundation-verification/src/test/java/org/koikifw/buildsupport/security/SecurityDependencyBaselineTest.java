package org.koikifw.buildsupport.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.SecurityFilterChain;

class SecurityDependencyBaselineTest {

    @Test
    void providesBootManagedProductionAndTestSecurityTypes() {
        assertThat(SecurityFilterChain.class).isNotNull();
        assertThat(SecurityMockMvcRequestPostProcessors.class).isNotNull();
    }
}
