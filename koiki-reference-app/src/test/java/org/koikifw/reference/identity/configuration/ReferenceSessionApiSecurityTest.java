package org.koikifw.reference.identity.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.Objects;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.koikifw.reference.identity.adapter.inbound.web.ReferenceSessionCsrfController;
import org.koikifw.session.internal.KoikiSessionJdbcAutoConfiguration;
import org.koikifw.session.internal.KoikiSessionJdbcDefaultsEnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.session.autoconfigure.SessionAutoConfiguration;
import org.springframework.boot.session.jdbc.autoconfigure.JdbcSessionAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/** Real Security chains and framework logout; no production failure switches or mock CSRF for SPA POSTs. */
class ReferenceSessionApiSecurityTest {
    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withInitializer(context -> new KoikiSessionJdbcDefaultsEnvironmentPostProcessor()
                    .postProcessEnvironment(context.getEnvironment(), new SpringApplication()))
            .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class,
                    ServletWebSecurityAutoConfiguration.class, SessionAutoConfiguration.class,
                    JdbcSessionAutoConfiguration.class, KoikiSessionJdbcAutoConfiguration.class,
                    org.koikifw.starter.security.internal.KoikiSecurityAutoConfiguration.class))
            .withUserConfiguration(ReferenceSecurityConfiguration.class, ReferenceApiSecurityConfiguration.class,
                    FixtureConfiguration.class)
            .withPropertyValues("server.servlet.session.cookie.secure=false",
                    "koiki.reference.api.bearer.enabled=true",
                    "koiki.reference.api.bearer.issuer=https://issuer.example.test",
                    "koiki.reference.api.bearer.audience=reference-api")
            .withBean(DataSource.class, () -> mock(DataSource.class))
            .withBean(org.koikifw.identity.IdentityQuery.class, () -> mock(org.koikifw.identity.IdentityQuery.class))
            .withBean(org.springframework.security.oauth2.jwt.JwtDecoder.class,
                    () -> mock(org.springframework.security.oauth2.jwt.JwtDecoder.class))
            .withBean(tools.jackson.databind.ObjectMapper.class, () -> tools.jackson.databind.json.JsonMapper.builder().build())
            .withBean(JdbcIndexedSessionRepository.class, () -> mock(JdbcIndexedSessionRepository.class));

    @ParameterizedTest
    @ValueSource(strings = {"/spa-api/csrf", "/spa-api/v1/expense-requests", "/spa-api/v2/unknown"})
    void anonymousGetIsJson401WithoutSessionCreation(String path) {
        runner.run(context -> {
            var result = mvc(context).perform(get(path).header("Authorization", "Bearer fixture-token"))
                    .andExpect(status().isUnauthorized()).andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.code").value("KOIKI-REF-SESSION-001"))
                    .andExpect(header().doesNotExist("Location")).andReturn();
            assertThat(result.getRequest().getSession(false)).isNull();
        });
    }

    @Test
    void authenticatedSessionWinsOverBearerAndPermissionIsEnforced() {
        runner.run(context -> {
            var mvc = mvc(context);
            mvc.perform(get("/spa-api/v1/expense-requests").with(user("reader").authorities(() -> "EXPENSE:APPLY"))
                            .header("Authorization", "Bearer other-user"))
                    .andExpect(status().isOk()).andExpect(content().string("reader"));
            mvc.perform(get("/spa-api/v1/expense-requests").with(user("viewer")))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("KOIKI-REF-SESSION-002"));
            var browserSession = new MockHttpSession();
            browserSession.setAttribute(
                    org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    new org.springframework.security.core.context.SecurityContextImpl(
                            org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                                    User.withUsername("reader").password("unused").authorities("EXPENSE:APPLY").build(),
                                    "", java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                            "EXPENSE:APPLY")))));
            // user() directly installs a request context and would bypass this transport boundary.
            mvc.perform(get("/api/v1/expense-requests").session(browserSession))
                    .andExpect(status().isUnauthorized());
            org.mockito.Mockito.verifyNoInteractions(context.getBean(org.springframework.security.oauth2.jwt.JwtDecoder.class));
        });
    }

    @ParameterizedTest
    @CsvSource({"false,false,403", "true,false,403", "false,true,403", "true,true,204"})
    void checksActualCookieAndHeader(boolean cookiePresent, boolean headerPresent, int expected) {
        runner.run(context -> {
            var mvc = mvc(context);
            var token = Objects.requireNonNull(mvc.perform(get("/spa-api/csrf").with(user("reader")))
                    .andReturn().getResponse().getCookie("XSRF-TOKEN"));
            var request = post("/spa-api/v1/expense-requests")
                    .with(user("reader").authorities(() -> "EXPENSE:APPLY"));
            if (cookiePresent) request.cookie(new Cookie("XSRF-TOKEN", token.getValue()));
            if (headerPresent) request.header("X-XSRF-TOKEN", token.getValue());
            var result = mvc.perform(request).andExpect(status().is(expected));
            if (expected == 403) result.andExpect(jsonPath("$.code").value("KOIKI-REF-CSRF-001"));
        });
    }

    @Test
    void rejectsMismatchedTokenAndAnonymousUnsafeRequest() {
        runner.run(context -> {
            var mvc = mvc(context);
            mvc.perform(post("/spa-api/v1/expense-requests").with(user("reader"))
                            .cookie(new Cookie("XSRF-TOKEN", "one")).header("X-XSRF-TOKEN", "two"))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("KOIKI-REF-CSRF-001"));
            mvc.perform(post("/spa-api/v1/expense-requests")).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("KOIKI-REF-CSRF-001"));
            mvc.perform(post("/spa-api/v1/expense-requests").cookie(new Cookie("XSRF-TOKEN", "same"))
                            .header("X-XSRF-TOKEN", "same"))
                    .andExpect(status().isUnauthorized());
        });
    }

    @Test
    void csrfCookieUsesExplicitSecureAndIsReadableSessionCookie() {
        runner.withPropertyValues("server.servlet.session.cookie.secure=true").run(context -> {
            var response = mvc(context).perform(get("/spa-api/csrf").with(user("reader")))
                    .andExpect(status().isNoContent()).andReturn().getResponse();
            var token = Objects.requireNonNull(response.getCookie("XSRF-TOKEN"));
            assertThat(token.getSecure()).isTrue();
            assertThat(token.isHttpOnly()).isFalse();
            assertThat(token.getPath()).isEqualTo("/");
            assertThat(token.getMaxAge()).isEqualTo(-1);
            // CookieCsrfTokenRepository uses Servlet 6 Cookie attributes; MockHttpServletResponse's
            // Set-Cookie string does not render these extension attributes.
            assertThat(token.getAttribute("SameSite")).isEqualTo("Lax");
        });
    }

    @ParameterizedTest
    @CsvSource({"true,false,strict,Strict", "true,true,omitted,none",
            "false,false,strict,Strict", "false,true,omitted,none"})
    void csrfCookieSettingsWinOverRequestScheme(boolean secure, boolean requestSecure,
            String sameSite, String expected) {
        runner.withPropertyValues("server.servlet.session.cookie.secure=" + secure,
                "server.servlet.session.cookie.same-site=" + sameSite).run(context -> {
                    var token = Objects.requireNonNull(mvc(context).perform(get("/spa-api/csrf")
                                    .secure(requestSecure).with(user("reader")))
                            .andReturn().getResponse().getCookie("XSRF-TOKEN"));
                    assertThat(token.getSecure()).isEqualTo(secure);
                    assertThat(token.getAttribute("SameSite")).isEqualTo(expected.equals("none") ? null : expected);
                });
    }

    @Test
    void committedLogoutFailureIsPropagatedWithoutRewritingSuccess() {
        runner.run(context -> {
            var boundary = new ReferenceSessionLogoutBoundary("/spa-api/logout",
                    context.getBean(org.springframework.security.web.csrf.CookieCsrfTokenRepository.class),
                    context.getBean(org.springframework.session.web.http.CookieSerializer.class),
                    context.getBean(ReferenceSessionApiProblemWriter.class));
            var request = new org.springframework.mock.web.MockHttpServletRequest("POST", "/spa-api/logout");
            var response = new org.springframework.mock.web.MockHttpServletResponse();
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> boundary.doFilter(request, response, (req, res) -> {
                res.flushBuffer();
                throw new IllegalStateException("private-detail");
            })).isInstanceOf(jakarta.servlet.ServletException.class)
                    .hasMessage("Reference logout response was already committed");
            assertThat(response.isCommitted()).isTrue();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getContentAsString()).doesNotContain("private-detail");
        });
    }

    @Test
    void loginClearsSpaTokenAndNextGetIssuesAnotherToken() {
        runner.run(context -> {
            var mvc = mvc(context);
            var login = mvc.perform(post("/login").with(csrf()).param("username", "reader").param("password", "password")
                            .cookie(new Cookie("XSRF-TOKEN", "old-token")))
                    .andExpect(status().is3xxRedirection()).andExpect(header().string("Location", "/"))
                    .andReturn();
            assertThat(login.getResponse().getHeaders("Set-Cookie"))
                    .anyMatch(value -> value.startsWith("XSRF-TOKEN=;") && value.contains("Max-Age=0"));
            var session = (MockHttpSession) Objects.requireNonNull(login.getRequest().getSession(false));
            var fresh = Objects.requireNonNull(mvc.perform(get("/spa-api/csrf").session(session))
                    .andExpect(status().isNoContent()).andReturn().getResponse().getCookie("XSRF-TOKEN"));
            assertThat(fresh.getValue()).isNotEqualTo("old-token");
            mvc.perform(post("/spa-api/v1/expense-requests").session(session)
                            .cookie(new Cookie("XSRF-TOKEN", fresh.getValue())).header("X-XSRF-TOKEN", "old-token"))
                    .andExpect(status().isForbidden());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"/logout", "/spa-api/logout"})
    void logoutDeletesBothCookiesAndInvalidatesSession(String path) {
        runner.run(context -> {
            var session = new MockHttpSession();
            var request = logout(path).session(session).with(user("reader"));
            var result = mvc(context).perform(request)
                    .andExpect(status().is(path.equals("/logout") ? 302 : 204)).andReturn();
            assertThat(session.isInvalid()).isTrue();
            assertThat(result.getResponse().getHeaders("Set-Cookie"))
                    .anyMatch(value -> value.startsWith("SESSION=;") && value.contains("Max-Age=0"))
                    .anyMatch(value -> value.startsWith("XSRF-TOKEN=;") && value.contains("Max-Age=0"));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"/logout", "/spa-api/logout"})
    void persistentLogoutFailureReturns503AfterLocalCleanup(String path) {
        runner.run(context -> {
            var session = spy(new MockHttpSession());
            doThrow(new IllegalStateException("private-store-detail")).when(session).invalidate();
            var result = mvc(context).perform(logout(path).session(session).with(user("reader")))
                    .andExpect(status().isServiceUnavailable()).andExpect(header().doesNotExist("Location")).andReturn();
            assertThat(result.getResponse().getContentAsString()).doesNotContain("private-store-detail");
            assertThat(result.getResponse().getHeaders("Set-Cookie"))
                    .anyMatch(value -> value.startsWith("XSRF-TOKEN=;") && value.contains("Max-Age=0"))
                    .anyMatch(value -> value.startsWith("SESSION=;") && value.contains("Max-Age=0"));
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            if (path.startsWith("/spa-api/")) {
                assertThat(result.getResponse().getContentAsString()).contains("KOIKI-REF-SESSION-003");
            }
        });
    }

    @Test
    void logoutRequiresPostAndCsrfAndAnonymousValidLogoutIsIdempotent() {
        runner.run(context -> {
            var mvc = mvc(context);
            mvc.perform(get("/spa-api/logout").with(user("reader"))).andExpect(status().isNotFound());
            mvc.perform(post("/spa-api/logout").with(user("reader"))).andExpect(status().isForbidden());
            mvc.perform(logout("/spa-api/logout")).andExpect(status().isNoContent());
        });
    }

    @Test
    void browserNamespaceKeepsLoginRedirectUnknown404AndFallbackDeny() {
        runner.run(context -> {
            var mvc = mvc(context);
            mvc.perform(get("/spa/unknown")).andExpect(status().is3xxRedirection());
            mvc.perform(get("/spa/unknown").with(user("reader"))).andExpect(status().isNotFound());
            mvc.perform(get("/spa/assets/missing.js")).andExpect(status().isNotFound());
            mvc.perform(get("/spa-api/v2/unknown").with(user("reader"))).andExpect(status().isNotFound());
            mvc.perform(get("/unregistered").with(user("reader"))).andExpect(status().isForbidden());
            assertThat(context.getBeansOfType(ReferenceSessionLogoutBoundary.class)).isEmpty();
        });
    }

    private static MockHttpServletRequestBuilder logout(String path) {
        var request = post(path);
        return path.equals("/logout") ? request.with(csrf())
                : request.cookie(new Cookie("XSRF-TOKEN", "fixture-token")).header("X-XSRF-TOKEN", "fixture-token");
    }

    private static MockMvc mvc(org.springframework.web.context.WebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @Import({ReferenceSessionCsrfController.class, FixtureController.class})
    static class FixtureConfiguration {
        @Bean
        @org.springframework.context.annotation.Primary
        org.springframework.security.authentication.password.CompromisedPasswordChecker fixturePasswordChecker() {
            return password -> new org.springframework.security.authentication.password.CompromisedPasswordDecision(false);
        }
        @Bean
        UserDetailsService users() {
            return new InMemoryUserDetailsManager(User.withUsername("reader").password("{noop}password")
                    .authorities("EXPENSE:APPLY").build());
        }
    }

    @RestController
    static class FixtureController {
        @GetMapping("/spa-api/v1/expense-requests")
        String list(java.security.Principal principal) { return principal.getName(); }
        @PostMapping("/spa-api/v1/expense-requests")
        org.springframework.http.ResponseEntity<Void> create() {
            return org.springframework.http.ResponseEntity.noContent().build();
        }
    }
}
