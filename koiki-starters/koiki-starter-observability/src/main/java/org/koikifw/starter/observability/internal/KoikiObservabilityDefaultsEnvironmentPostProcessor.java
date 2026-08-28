package org.koikifw.starter.observability.internal;

import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/** Adds low-precedence structured logging defaults while preserving application overrides. */
public final class KoikiObservabilityDefaultsEnvironmentPostProcessor
        implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "koikiObservabilityDefaults";

    private static final Map<String, Object> DEFAULTS = Map.of(
            "logging.structured.format.console", "logstash",
            "logging.structured.json.rename.[@timestamp]", "timestamp",
            "logging.structured.json.add.service", "${spring.application.name:application}",
            "logging.structured.json.add.environment", "${koiki.environment:default}");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean enabled = environment.getProperty("koiki.observability.enabled", Boolean.class, true);
        if (enabled) {
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, DEFAULTS));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
