package org.koikifw.buildsupport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.identity.UserSessionInvalidator;
import org.koikifw.session.internal.KoikiSessionJdbcAutoConfiguration;
import org.koikifw.session.internal.KoikiSessionJdbcDefaultsEnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.transaction.PlatformTransactionManager;

class SessionJdbcAutoConfigurationContextTest {

    private static final String[] APPROVED_VALUES = {
        "spring.session.jdbc.initialize-schema=never",
        "spring.session.jdbc.table-name=koiki_session",
        "spring.session.jdbc.cleanup-cron=-",
        "server.servlet.session.cookie.http-only=true"
    };

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KoikiSessionJdbcAutoConfiguration.class))
            .withBean(DataSource.class, () -> mock(DataSource.class))
            .withBean(PlatformTransactionManager.class,
                    () -> mock(PlatformTransactionManager.class))
            .withPropertyValues(APPROVED_VALUES);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void suppliesTheApprovedSpringSessionAndCookieDefaults() {
        StandardEnvironment environment = new StandardEnvironment();

        new KoikiSessionJdbcDefaultsEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.session.jdbc.initialize-schema"))
                .isEqualTo("never");
        assertThat(environment.getProperty("spring.session.jdbc.table-name"))
                .isEqualTo("koiki_session");
        assertThat(environment.getProperty("spring.session.jdbc.cleanup-cron"))
                .isEqualTo("-");
        assertThat(environment.getProperty("spring.session.jdbc.flush-mode"))
                .isEqualTo("on-save");
        assertThat(environment.getProperty("spring.session.jdbc.save-mode"))
                .isEqualTo("on-set-attribute");
        assertThat(environment.getProperty("server.servlet.session.cookie.http-only", Boolean.class))
                .isTrue();
        assertThat(environment.getProperty("server.servlet.session.cookie.secure", Boolean.class))
                .isTrue();
        assertThat(environment.getProperty("server.servlet.session.cookie.same-site"))
                .isEqualTo("lax");
    }

    @Test
    void keepsApplicationOverridesVisibleForTheStartupGuard() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "fixtureApplication", Map.of("spring.session.jdbc.table-name", "other_session")));

        new KoikiSessionJdbcDefaultsEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.session.jdbc.table-name"))
                .isEqualTo("other_session");
    }

    @Test
    void acceptsOnlyTheApprovedSchemaAndWebCleanupBoundary() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("koikiSessionJdbcSettingsGuard");
            assertThat(context).hasSingleBean(JdbcIndexedSessionRepository.class);
            assertThat(context).hasSingleBean(org.koikifw.session.SessionCleanup.class);
        });

        assertRejected("spring.session.jdbc.initialize-schema=always", "initialize-schema");
        assertRejected("spring.session.jdbc.table-name=other_session", "table-name");
        assertRejected("spring.session.jdbc.cleanup-cron=0 * * * * *", "cleanup-cron");
        assertRejected("server.servlet.session.cookie.http-only=false", "http-only");
    }

    @Test
    void suppliesTheInvalidatorAndPreservesTheSessionStoreFailure() {
        JdbcIndexedSessionRepository repository = mock(JdbcIndexedSessionRepository.class);
        when(repository.findByIndexNameAndIndexValue(any(), any()))
                .thenThrow(new DataAccessResourceFailureException("fixture store unavailable"));

        runner.withBean(JdbcIndexedSessionRepository.class, () -> repository)
                .run(context -> {
                    assertThat(context).hasSingleBean(UserSessionInvalidator.class);
                    UserSessionInvalidator invalidator = context.getBean(UserSessionInvalidator.class);
                    assertThatThrownBy(() -> invalidator.invalidateAll(FrameworkUserId.parse(
                                    "00000000-0000-4000-8000-000000000303")))
                            .isInstanceOf(DataAccessResourceFailureException.class);
                });
    }

    @Test
    void clearsLocalLogoutStateButDoesNotHidePersistentStoreFailure() {
        CookieSerializer cookieSerializer = mock(CookieSerializer.class);
        JdbcIndexedSessionRepository repository = mock(JdbcIndexedSessionRepository.class);
        WebApplicationContextRunner webRunner = new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(KoikiSessionJdbcAutoConfiguration.class))
                .withBean(DataSource.class, () -> mock(DataSource.class))
                .withBean(JdbcIndexedSessionRepository.class, () -> repository)
                .withBean(CookieSerializer.class, () -> cookieSerializer)
                .withPropertyValues(APPROVED_VALUES);

        webRunner.run(context -> {
            LogoutHandler logoutHandler = context.getBean("koikiSessionLogoutHandler", LogoutHandler.class);
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpSession session = mock(HttpSession.class);
            when(request.getSession(false)).thenReturn(session);
            doThrow(new IllegalStateException("fixture store unavailable"))
                    .when(session)
                    .invalidate();
            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated("principal", "credential", java.util.List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            assertThatThrownBy(() -> logoutHandler.logout(
                            request, new MockHttpServletResponse(), authentication))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("KOIKI persistent session logout failed")
                    .hasMessageNotContaining("fixture");
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(authentication.getCredentials()).isNull();
            verify(cookieSerializer).writeCookieValue(any(CookieSerializer.CookieValue.class));
        });
    }

    private void assertRejected(String property, String expectedMessagePart) {
        runner.withPropertyValues(property).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .rootCause()
                    .hasMessageContaining(expectedMessagePart)
                    .hasMessageNotContaining("always")
                    .hasMessageNotContaining("other_session");
        });
    }
}
