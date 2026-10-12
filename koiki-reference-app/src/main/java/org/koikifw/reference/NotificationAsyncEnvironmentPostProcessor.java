package org.koikifw.reference;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.ClassUtils;

/** Applies the R1 opt-in before auto-configuration and Flyway can create side effects. */
public final class NotificationAsyncEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private static final List<String> REGISTRY_CONFIGURATIONS = List.of(
            "org.springframework.modulith.events.config.EventPublicationAutoConfiguration",
            "org.springframework.modulith.events.config.EventExternalizationAutoConfiguration",
            "org.springframework.modulith.events.jdbc.JdbcEventPublicationAutoConfiguration");

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String selected = environment.getProperty("koiki.reference.notification.async.enabled", "false");
        if (!selected.equals("true") && !selected.equals("false")) {
            throw new IllegalArgumentException("Invalid Reference async enabled property");
        }
        var values = new LinkedHashMap<String, Object>();
        var excludes = new LinkedHashSet<>(Binder.get(environment)
                .bind("spring.autoconfigure.exclude", Bindable.listOf(String.class)).orElse(List.of()));
        if (selected.equals("false")) {
            excludes.addAll(REGISTRY_CONFIGURATIONS);
            values.put("spring.autoconfigure.exclude", String.join(",", excludes));
        } else {
            if (!ClassUtils.isPresent("org.springframework.modulith.events.jdbc.JdbcEventPublicationRepository",
                    getClass().getClassLoader())) {
                throw new IllegalStateException("Reference async selection requires the JDBC starter artifact");
            }
            if (excludes.stream().anyMatch(REGISTRY_CONFIGURATIONS::contains)) {
                throw new IllegalStateException("Selected Reference async registry is explicitly excluded");
            }
            Properties properties = new Properties();
            try (var resource = getClass().getResourceAsStream("/application-s1-reference-async.properties")) {
                if (resource == null) {
                    throw new IllegalStateException("Reference async settings absent");
                }
                properties.load(resource);
            } catch (IOException exception) {
                throw new IllegalStateException("Reference async settings unavailable", exception);
            }
            properties.forEach((key, value) -> values.put(key.toString(), value));
            var locations = new LinkedHashSet<>(Binder.get(environment)
                    .bind("spring.flyway.locations", Bindable.listOf(String.class))
                    .orElse(List.of("classpath:db/migration/kkref")));
            locations.add("classpath:db/migration/kkref-notification-async");
            values.put("spring.flyway.locations", String.join(",", locations));
            values.put("spring.autoconfigure.exclude", String.join(",", excludes));
        }
        environment.getPropertySources().addFirst(new MapPropertySource("referenceNotificationAsyncGate", values));
    }
}
