package org.koikifw.reference.externalcheck.configuration;

import java.net.URI;
import java.time.Duration;
import org.koikifw.reference.externalcheck.adapter.outbound.external.HttpExternalCheckAdapter;
import org.koikifw.reference.externalcheck.adapter.outbound.external.RetryingRateLookup;
import org.koikifw.reference.externalcheck.application.ExternalCheckService;
import org.koikifw.reference.externalcheck.application.port.outbound.ExternalCheckPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import tools.jackson.databind.ObjectMapper;

/** Reference-only opt-in assembly. No endpoint or client is created by default. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "koiki.reference.external-check.enabled", havingValue = "true")
public class ExternalCheckConfiguration {
    @Bean
    ExternalCheckProperties externalCheckProperties(Environment environment) {
        if (!environment.getProperty("koiki.api.enabled", Boolean.class, true)
                || !environment.getProperty("koiki.api.resilience.enabled", Boolean.class, true)) {
            throw new IllegalArgumentException("External check requires KOIKI resilience processing");
        }
        int retries = environment.getRequiredProperty("koiki.api.resilience.retry.max-retries", Integer.class);
        Duration delay = environment.getRequiredProperty("koiki.api.resilience.retry.delay", Duration.class);
        Duration timeout = environment.getRequiredProperty("koiki.api.resilience.retry.timeout", Duration.class);
        if (retries < 0 || retries > 2 || delay.isNegative() || delay.compareTo(Duration.ofMillis(10)) > 0
                || timeout.toMillis() < 1 || timeout.compareTo(Duration.ofSeconds(1)) > 0) {
            throw new IllegalArgumentException("External check requires bounded retry settings");
        }
        return new ExternalCheckProperties(URI.create(environment.getRequiredProperty(
                "koiki.reference.external-check.provider-base-uri")),
                environment.getRequiredProperty("koiki.api.http.connect-timeout", Duration.class),
                environment.getRequiredProperty("koiki.api.http.read-timeout", Duration.class));
    }

    @Bean(destroyMethod = "close")
    HttpExternalCheckAdapter httpExternalCheckAdapter(ExternalCheckProperties properties, ObjectMapper mapper) {
        return new HttpExternalCheckAdapter(properties, mapper);
    }

    @Bean
    ExternalCheckPort externalCheckPort(HttpExternalCheckAdapter adapter) { return new RetryingRateLookup(adapter); }

    @Bean
    ExternalCheckService externalCheckService(ExternalCheckPort port) { return new ExternalCheckService(port); }
}
