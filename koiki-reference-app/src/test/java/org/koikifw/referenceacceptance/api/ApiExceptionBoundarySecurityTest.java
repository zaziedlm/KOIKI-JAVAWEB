package org.koikifw.referenceacceptance.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.koikifw.starter.api.internal.KoikiApiAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/** Exercises the API advice through real filters and method security, without local advice. */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = ApiExceptionBoundarySecurityTest.TestConfiguration.class)
class ApiExceptionBoundarySecurityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void configureMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/boundary", "/mvc/boundary"})
    void authenticatedMethodDenialReachesTheConfiguredSecurityHandler(String base) throws Exception {
        mvc.perform(get(base + "/method").with(user("reader")))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-Fixture-Handler", base.startsWith("/api/") ? "api" : "mvc"));
    }

    @Test
    void anonymousMethodDenialUsesApiEntryPoint() throws Exception {
        mvc.perform(get("/api/boundary/method"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousMethodDenialUsesMvcLoginRedirect() throws Exception {
        mvc.perform(get("/mvc/boundary/method"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/login")));
    }

    @Test
    void methodDenialPreservesTheHtmxHtmlHandler() throws Exception {
        mvc.perform(get("/mvc/boundary/method").with(user("reader")).header("HX-Request", "true"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string("<section>fixture-denied</section>"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/boundary", "/mvc/boundary"})
    void authenticationFailureUsesTheConfiguredEntryPoint(String base) throws Exception {
        ResultActions result = mvc.perform(get(base + "/authentication").with(user("reader")));
        if (base.startsWith("/api/")) {
            result.andExpect(status().isUnauthorized());
        } else {
            result.andExpect(status().is3xxRedirection())
                    .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/login")));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/boundary", "/mvc/boundary"})
    void wrappedSecurityFailureStillReachesSecurity(String base) throws Exception {
        mvc.perform(get(base + "/wrapped").with(user("reader")))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-Fixture-Handler", base.startsWith("/api/") ? "api" : "mvc"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/boundary", "/mvc/boundary"})
    void unexpectedFailureKeepsSanitizedProblemDetails(String base) throws Exception {
        mvc.perform(get(base + "/unexpected").with(user("reader")))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("KOIKI-INTERNAL-001"))
                .andExpect(content().string(not(containsString("fixture-private-detail"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/boundary", "/mvc/boundary"})
    void authorizedMethodStillSucceeds(String base) throws Exception {
        mvc.perform(get(base + "/method").with(user("admin").authorities(() -> "BOUNDARY:ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    @Import(KoikiApiAutoConfiguration.class)
    static class TestConfiguration {

        @Bean
        @Order(0)
        SecurityFilterChain api(HttpSecurity http) throws Exception {
            http.securityMatcher("/api/**");
            http.sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            http.requestCache(cache -> cache.disable());
            // Permit the URL so method-security exceptions actually reach the advice.
            http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
            http.exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                    .accessDeniedHandler((request, response, exception) -> {
                        response.setHeader("X-Fixture-Handler", "api");
                        response.setStatus(403);
                    }));
            return http.build();
        }

        @Bean
        @Order(1)
        SecurityFilterChain mvc(HttpSecurity http) throws Exception {
            http.securityMatcher("/mvc/**");
            http.requestCache(cache -> cache.disable());
            http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
            http.exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                    .defaultAccessDeniedHandlerFor((request, response, exception) -> {
                        response.setStatus(403);
                        response.setContentType(MediaType.TEXT_HTML_VALUE);
                        response.getWriter().write("<section>fixture-denied</section>");
                    }, new RequestHeaderRequestMatcher("HX-Request", "true"))
                    .defaultAccessDeniedHandlerFor((request, response, exception) -> {
                        response.setHeader("X-Fixture-Handler", "mvc");
                        response.setStatus(403);
                    }, AnyRequestMatcher.INSTANCE));
            return http.build();
        }

        @Bean
        SecuredOperations operations() {
            return new SecuredOperations();
        }

        @Bean
        FixtureController controller(SecuredOperations operations) {
            return new FixtureController(operations);
        }
    }

    static class SecuredOperations {

        @PreAuthorize("hasAuthority('BOUNDARY:ADMIN')")
        public String adminOnly() {
            return "ok";
        }
    }

    @RestController
    static class FixtureController {

        private final SecuredOperations operations;

        FixtureController(SecuredOperations operations) {
            this.operations = operations;
        }

        @GetMapping({"/api/boundary/method", "/mvc/boundary/method"})
        String method() {
            return operations.adminOnly();
        }

        @GetMapping({"/api/boundary/authentication", "/mvc/boundary/authentication"})
        String authentication() {
            throw new BadCredentialsException("fixture-private-detail");
        }

        @GetMapping({"/api/boundary/wrapped", "/mvc/boundary/wrapped"})
        String wrapped() {
            throw new IllegalStateException("fixture-private-detail", new AccessDeniedException("denied"));
        }

        @GetMapping({"/api/boundary/unexpected", "/mvc/boundary/unexpected"})
        String unexpected() {
            throw new IllegalStateException("fixture-private-detail");
        }
    }
}
