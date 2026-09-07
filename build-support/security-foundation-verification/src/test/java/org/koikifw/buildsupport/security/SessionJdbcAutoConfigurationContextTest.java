package org.koikifw.buildsupport.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.koikifw.session.internal.KoikiSessionJdbcAutoConfiguration;
import org.koikifw.session.internal.KoikiSessionJdbcDefaultsEnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

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
            .withPropertyValues(APPROVED_VALUES);

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
        });

        assertRejected("spring.session.jdbc.initialize-schema=always", "initialize-schema");
        assertRejected("spring.session.jdbc.table-name=other_session", "table-name");
        assertRejected("spring.session.jdbc.cleanup-cron=0 * * * * *", "cleanup-cron");
        assertRejected("server.servlet.session.cookie.http-only=false", "http-only");
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
