package org.koikifw.buildsupport.internal.runtimefoundation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.koikifw.starter.observability.internal.KoikiObservabilityDefaultsEnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class ObservabilityStarterDefaultsTest {

    private final KoikiObservabilityDefaultsEnvironmentPostProcessor postProcessor =
            new KoikiObservabilityDefaultsEnvironmentPostProcessor();

    @Test
    void suppliesStructuredLoggingDefaults() {
        StandardEnvironment environment = new StandardEnvironment();

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("logging.structured.format.console")).isEqualTo("logstash");
        assertThat(environment.getProperty("logging.structured.json.rename.[@timestamp]")).isEqualTo("timestamp");
        assertThat(environment.getProperty("logging.structured.json.add.service")).isEqualTo("application");
        assertThat(environment.getProperty("logging.structured.json.add.environment")).isEqualTo("default");
    }

    @Test
    void preservesApplicationOwnedLoggingFormatAndContextValues() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "applicationOverride",
                Map.of(
                        "logging.structured.format.console", "ecs",
                        "spring.application.name", "customer-service",
                        "koiki.environment", "acceptance")));

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("logging.structured.format.console")).isEqualTo("ecs");
        assertThat(environment.getProperty("logging.structured.json.add.service"))
                .isEqualTo("customer-service");
        assertThat(environment.getProperty("logging.structured.json.add.environment"))
                .isEqualTo("acceptance");
    }

    @Test
    void suppliesNoLoggingDefaultsWhenStarterIsDisabled() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "disableObservability",
                Map.of("koiki.observability.enabled", false)));

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("logging.structured.format.console")).isNull();
        assertThat(environment.getProperty("logging.structured.json.add.service")).isNull();
    }
}
