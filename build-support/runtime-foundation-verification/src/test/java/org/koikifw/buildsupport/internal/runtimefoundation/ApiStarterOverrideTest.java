package org.koikifw.buildsupport.internal.runtimefoundation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest(
        classes = ApiStarterSmokeTest.TestApplication.class,
        properties = {
            "spring.jackson.find-and-add-modules=true",
            "spring.mvc.apiversion.supported=2",
            "koiki.api.resilience.retry.max-retries=4"
        })
class ApiStarterOverrideTest {

    @Autowired
    private Environment environment;

    @Autowired
    private JacksonProperties jacksonProperties;

    @Test
    void applicationPropertiesOverrideLowPrecedenceKoikiDefaults() {
        assertThat(jacksonProperties.isFindAndAddModules()).isTrue();
        assertThat(environment.getProperty("spring.jackson.find-and-add-modules"))
                .isEqualTo("true");
        assertThat(environment.getProperty("spring.mvc.apiversion.supported"))
                .isEqualTo("2");
        assertThat(environment.getProperty("koiki.api.resilience.retry.max-retries"))
                .isEqualTo("4");
    }
}
