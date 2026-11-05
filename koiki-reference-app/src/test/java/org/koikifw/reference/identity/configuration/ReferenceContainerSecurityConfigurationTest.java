package org.koikifw.reference.identity.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.koikifw.starter.security.internal.KoikiSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

class ReferenceContainerSecurityConfigurationTest {
    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class,
                    ServletWebSecurityAutoConfiguration.class, KoikiSecurityAutoConfiguration.class))
            .withUserConfiguration(ReferenceContainerSecurityConfiguration.class, Fixture.class);

    @Test
    void probeChainIsAbsentWithoutExplicitProfile() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean("referenceContainerProbeSecurityFilterChain");
            MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build()
                    .perform(get("/actuator/health/liveness")).andExpect(status().isUnauthorized());
        });
    }

    @Test
    void profileAllowsOnlyGetOnExactProbePaths() {
        runner.withPropertyValues("spring.profiles.active=d3-container").run(context -> {
            var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
            mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
            mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
            mvc.perform(post("/actuator/health/liveness")).andExpect(status().isForbidden());
            mvc.perform(get("/actuator/health")).andExpect(status().isUnauthorized());
            mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
            mvc.perform(get("/unmatched")).andExpect(status().isUnauthorized());
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import(Probe.class)
    static class Fixture {}

    @RestController
    static class Probe {
        @GetMapping({"/actuator/health/liveness", "/actuator/health/readiness"})
        String health() { return "UP"; }
    }
}
