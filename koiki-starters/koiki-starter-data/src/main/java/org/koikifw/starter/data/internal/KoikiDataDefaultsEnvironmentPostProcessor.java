package org.koikifw.starter.data.internal;

import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/** Adds low-precedence owner-specific Flyway defaults while preserving application overrides. */
public final class KoikiDataDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "koikiDataDefaults";

    private static final Map<String, Object> DEFAULTS = Map.of(
            "spring.flyway.locations", "classpath:db/migration/customer",
            "spring.flyway.table", "flyway_schema_history",
            "spring.flyway.baseline-on-migrate", true,
            "spring.flyway.baseline-version", "0");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean dataEnabled = environment.getProperty("koiki.data.enabled", Boolean.class, true);
        boolean flywayEnabled = environment.getProperty("koiki.data.flyway.enabled", Boolean.class, true);
        if (dataEnabled && flywayEnabled) {
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, DEFAULTS));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
