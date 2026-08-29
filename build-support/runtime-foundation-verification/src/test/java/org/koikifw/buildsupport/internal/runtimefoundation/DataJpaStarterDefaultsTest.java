package org.koikifw.buildsupport.internal.runtimefoundation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.koikifw.starter.data.jpa.internal.KoikiDataJpaDefaultsEnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class DataJpaStarterDefaultsTest {

    private final KoikiDataJpaDefaultsEnvironmentPostProcessor postProcessor =
            new KoikiDataJpaDefaultsEnvironmentPostProcessor();

    @Test
    void disablesOpenEntityManagerInViewByDefault() {
        StandardEnvironment environment = new StandardEnvironment();

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.jpa.open-in-view", Boolean.class)).isFalse();
    }

    @Test
    void preservesApplicationOwnedOpenEntityManagerInViewOverride() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "applicationOverride", Map.of("spring.jpa.open-in-view", true)));

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.jpa.open-in-view", Boolean.class)).isTrue();
    }

    @Test
    void suppliesNoJpaDefaultWhenProfileIsDisabled() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(
                "disableKoikiJpa", Map.of("koiki.data.jpa.enabled", false)));

        postProcessor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.jpa.open-in-view")).isNull();
    }
}
