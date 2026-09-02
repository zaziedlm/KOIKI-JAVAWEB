package org.koikifw.buildsupport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.koikifw.starter.security.internal.KoikiSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

class SecurityRequestBoundaryTest {

    private static final String SENSITIVE_MARKER = "fixture-sensitive-credential-6f41";

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    KoikiSecurityAutoConfiguration.class,
                    SecurityAutoConfiguration.class,
                    ServletWebSecurityAutoConfiguration.class))
            .withUserConfiguration(FixtureWebConfiguration.class);

    @Test
    void fallbackReturns401ForAnonymousAnd403ForAuthenticatedRequest() {
        runner.run(context -> {
            MockMvc mvc = mockMvc(context);

            mvc.perform(get("/fixture/unmatched"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string(""));
            mvc.perform(get("/fixture/unmatched").with(user("fixture-user")))
                    .andExpect(status().isForbidden())
                    .andExpect(content().string(""));
        });
    }

    @Test
    void fallbackKeepsCsrfAndSecurityHeadersEnabled() {
        runner.run(context -> {
            MockMvc mvc = mockMvc(context);

            mvc.perform(post("/fixture/unmatched").with(user("fixture-user")))
                    .andExpect(status().isForbidden())
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"));
            mvc.perform(post("/fixture/unmatched").with(user("fixture-user")).with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"));
        });
    }

    @Test
    void credentialShapedInputNeverActivatesFallbackAuthenticationOrLeaks() {
        runner.run(context -> {
            MockMvc mvc = mockMvc(context);

            MvcResult result = mvc.perform(get("/fixture/unmatched")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + SENSITIVE_MARKER)
                            .header("X-Amzn-Oidc-Data", SENSITIVE_MARKER)
                            .cookie(new jakarta.servlet.http.Cookie("fixture-session", SENSITIVE_MARKER)))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).doesNotContain(SENSITIVE_MARKER);
            assertThat(result.getResponse().getHeaderNames())
                    .allSatisfy(name -> assertThat(result.getResponse().getHeaders(name))
                            .noneMatch(value -> value.contains(SENSITIVE_MARKER)));
        });
    }

    @Test
    void customerChainHandlesItsMatcherAndFallbackDeniesEverythingElse() {
        runner.withUserConfiguration(CustomerChainConfiguration.class).run(context -> {
            MockMvc mvc = mockMvc(context);

            mvc.perform(get("/fixture/customer/read").with(user("fixture-user")))
                    .andExpect(status().isOk())
                    .andExpect(content().string("fixture-ok"));
            mvc.perform(get("/fixture/customer/read"))
                    .andExpect(status().isUnauthorized());
            mvc.perform(get("/fixture/unmatched").with(user("fixture-user")))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/fixture/customer/write").with(user("fixture-user")))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/fixture/customer/write").with(user("fixture-user")).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(content().string("fixture-ok"));
        });
    }

    private static MockMvc mockMvc(WebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class FixtureWebConfiguration {

        @Bean
        FixtureController fixtureController() {
            return new FixtureController();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomerChainConfiguration {

        @Bean
        @Order(1)
        SecurityFilterChain customerSecurityFilterChain(HttpSecurity http) throws Exception {
            http.securityMatcher("/fixture/customer/**");
            http.authorizeHttpRequests(requests -> requests.anyRequest().authenticated());
            http.exceptionHandling(exceptions ->
                    exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
            return http.build();
        }
    }

    @RestController
    static class FixtureController {

        @GetMapping({"/fixture/customer/read", "/fixture/unmatched"})
        String read() {
            return "fixture-ok";
        }

        @PostMapping({"/fixture/customer/write", "/fixture/unmatched"})
        String write() {
            return "fixture-ok";
        }
    }
}
