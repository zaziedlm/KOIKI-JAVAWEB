package org.koikifw.buildsupport.internal.runtimefoundation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.koikifw.starter.data.internal.KoikiDataDefaultsEnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class DataStarterDefaultsTest {

    private final KoikiDataDefaultsEnvironmentPostProcessor postProcessor =
            new KoikiDataDefaultsEnvironmentPostProcessor();

    @Test
    void suppliesOwnerSpecificCustomerDefaultsAtLowPrecedence() {
        StandardEnvironment environment = new StandardEnvironment();

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.flyway.locations"))
                .isEqualTo("classpath:db/migration/customer");
        assertThat(environment.getProperty("spring.flyway.table"))
                .isEqualTo("flyway_schema_history");
        assertThat(environment.getProperty("spring.flyway.baseline-on-migrate", Boolean.class))
                .isTrue();
        assertThat(environment.getProperty("spring.flyway.baseline-version"))
                .isEqualTo("0");
    }

    @Test
    void preservesApplicationOwnedOverrides() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "applicationOverride",
                Map.of("spring.flyway.locations", "classpath:db/migration/application")));

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.flyway.locations"))
                .isEqualTo("classpath:db/migration/application");
    }

    @Test
    void suppliesNoDefaultsWhenIntegrationIsDisabled() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "disableKoikiData",
                Map.of("koiki.data.flyway.enabled", false)));

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.flyway.locations")).isNull();
        assertThat(environment.getProperty("spring.flyway.table")).isNull();
    }
}
