package org.koikifw.referenceacceptance.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.koikifw.reference.identity.configuration.ReferenceSecurityConfiguration;
import org.koikifw.session.internal.KoikiSessionJdbcAutoConfiguration;
import org.koikifw.session.internal.KoikiSessionJdbcDefaultsEnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.session.autoconfigure.SessionAutoConfiguration;
import org.springframework.boot.session.jdbc.autoconfigure.JdbcSessionAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/** Checks serialized cookies and logout with Boot and Framework configurations together. */
class ReferenceSessionCookieConfigurationTest {
    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withInitializer(context -> new KoikiSessionJdbcDefaultsEnvironmentPostProcessor()
                    .postProcessEnvironment(context.getEnvironment(), new SpringApplication()))
            .withConfiguration(AutoConfigurations.of(
                    SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class,
                    SessionAutoConfiguration.class, JdbcSessionAutoConfiguration.class,
                    KoikiSessionJdbcAutoConfiguration.class))
            .withUserConfiguration(ReferenceSecurityConfiguration.class, FixtureConfiguration.class)
            .withBean(DataSource.class, () -> mock(DataSource.class))
            .withBean(JdbcIndexedSessionRepository.class, () -> mock(JdbcIndexedSessionRepository.class));

    @Test
    void appliesFrameworkCookieDefaultsAndSuppliesLogoutBeans() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(CookieSerializer.class);
            assertThat(context).hasBean("koikiSessionLogoutHandler");
            assertThat(context).hasBean("koikiSessionLogoutCustomizer");
            assertThat(serialize(context.getBean(CookieSerializer.class), false))
                    .startsWith("SESSION=").contains("Path=/", "Secure", "HttpOnly", "SameSite=Lax");
        });
    }

    @ParameterizedTest
    @CsvSource({"true,false", "true,true", "false,false", "false,true"})
    void explicitSecureSettingWinsOverRequestScheme(boolean secure, boolean requestSecure) {
        runner.withPropertyValues("server.servlet.session.cookie.secure=" + secure,
                        "server.servlet.session.cookie.same-site=strict")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    String cookie = serialize(context.getBean(CookieSerializer.class), requestSecure);
                    assertThat(cookie.contains("; Secure")).isEqualTo(secure);
                    assertThat(cookie).contains("HttpOnly", "SameSite=Strict");
                });
    }

    @Test
    void supportsExplicitlyOmittingSameSite() {
        runner.withPropertyValues("server.servlet.session.cookie.same-site=omitted")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(serialize(context.getBean(CookieSerializer.class), false))
                            .doesNotContain("SameSite=");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/fixture-session"})
    void logoutDeletesTheConfiguredCookieAndInvalidatesTheLocalSession(String path) {
        runner.withPropertyValues("server.servlet.session.cookie.name=REFERENCE_SESSION",
                        "server.servlet.session.cookie.path=" + path,
                        "server.servlet.session.cookie.secure=false",
                        "server.servlet.session.cookie.same-site=strict",
                        "server.servlet.session.cookie.max-age=10m")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(serialize(context.getBean(CookieSerializer.class), false))
                            .startsWith("REFERENCE_SESSION=")
                            .contains("Path=" + path + ";", "Max-Age=600", "HttpOnly", "SameSite=Strict")
                            .doesNotContain("; Secure");
                    var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
                    var session = new MockHttpSession();
                    var response = mvc.perform(post("/logout").session(session).with(user("fixture")).with(csrf()))
                            .andExpect(status().is3xxRedirection()).andReturn().getResponse();
                    assertThat(session.isInvalid()).isTrue();
                    assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
                            .anySatisfy(cookie -> assertThat(cookie).startsWith("REFERENCE_SESSION=")
                                    .contains("Path=" + path + ";", "Max-Age=0", "HttpOnly", "SameSite=Strict")
                                    .doesNotContain("; Secure"));
                });
    }

    private static String serialize(CookieSerializer serializer, boolean secure) {
        var request = new MockHttpServletRequest();
        request.setSecure(secure);
        var response = new MockHttpServletResponse();
        serializer.writeCookieValue(new CookieSerializer.CookieValue(request, response, "fixture-session"));
        return java.util.Objects.requireNonNull(response.getHeader(HttpHeaders.SET_COOKIE));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableConfigurationProperties(ServerProperties.class)
    static class FixtureConfiguration {
        @Bean
        UserDetailsService users() {
            return new InMemoryUserDetailsManager(User.withUsername("fixture")
                    .password("{noop}unused").roles("USER").build());
        }
    }
}
