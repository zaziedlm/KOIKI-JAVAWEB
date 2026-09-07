package org.koikifw.session.internal;

import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/** Adds low-precedence Spring Session JDBC defaults for the Framework-owned schema. */
public final class KoikiSessionJdbcDefaultsEnvironmentPostProcessor
        implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "koikiSessionJdbcDefaults";

    private static final Map<String, Object> DEFAULTS = Map.ofEntries(
            Map.entry("spring.session.jdbc.initialize-schema", "never"),
            Map.entry("spring.session.jdbc.table-name", "koiki_session"),
            Map.entry("spring.session.jdbc.cleanup-cron", "-"),
            Map.entry("spring.session.jdbc.flush-mode", "on-save"),
            Map.entry("spring.session.jdbc.save-mode", "on-set-attribute"),
            Map.entry("server.servlet.session.cookie.http-only", true),
            Map.entry("server.servlet.session.cookie.secure", true),
            Map.entry("server.servlet.session.cookie.same-site", "lax"));

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, DEFAULTS));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
