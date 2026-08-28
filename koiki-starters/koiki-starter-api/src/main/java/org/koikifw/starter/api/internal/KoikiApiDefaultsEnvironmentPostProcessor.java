package org.koikifw.starter.api.internal;

import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/** Adds low-precedence KOIKI API defaults while preserving application overrides. */
public final class KoikiApiDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String PROPERTY_SOURCE_NAME = "koikiApiDefaults";

    private static final Map<String, Object> DEFAULTS = Map.ofEntries(
            Map.entry("spring.jackson.find-and-add-modules", false),
            Map.entry("spring.jackson.deserialization.fail-on-unknown-properties", true),
            Map.entry("spring.jackson.deserialization.fail-on-trailing-tokens", true),
            Map.entry("spring.mvc.apiversion.use.path-segment", 1),
            Map.entry("spring.mvc.apiversion.required", true),
            Map.entry("spring.mvc.apiversion.detect-supported", false),
            Map.entry("spring.mvc.apiversion.supported", "1"),
            Map.entry("koiki.api.resilience.retry.max-retries", 2),
            Map.entry("koiki.api.resilience.retry.delay", "10ms"),
            Map.entry("koiki.api.resilience.retry.timeout", "1s"));

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getProperty("koiki.api.enabled", Boolean.class, true)) {
            environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, DEFAULTS));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
