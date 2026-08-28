package org.koikifw.buildsupport.internal.runtimefoundation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.resilience.annotation.RetryAnnotationBeanPostProcessor;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@SpringBootTest(
        classes = ApiStarterSmokeTest.TestApplication.class,
        properties = "koiki.api.resilience.enabled=false")
class ApiResilienceDisabledTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private JacksonProperties jacksonProperties;

    @Test
    void disablesOnlyResilienceWhileKeepingOtherApiDefaults() {
        assertThat(applicationContext.getBeansOfType(RetryAnnotationBeanPostProcessor.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(ResponseEntityExceptionHandler.class)).hasSize(1);
        assertThat(jacksonProperties.isFindAndAddModules()).isFalse();
        assertThat(applicationContext.getEnvironment().getProperty("spring.mvc.apiversion.use.path-segment"))
                .isEqualTo("1");
    }
}
