package org.koikifw.starter.data.jpa.internal;

import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/** Adds low-precedence JPA defaults while preserving application overrides. */
public final class KoikiDataJpaDefaultsEnvironmentPostProcessor
        implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "koikiDataJpaDefaults";

    private static final Map<String, Object> DEFAULTS = Map.of(
            "spring.jpa.open-in-view", false);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean enabled = environment.getProperty("koiki.data.jpa.enabled", Boolean.class, true);
        if (enabled) {
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, DEFAULTS));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
