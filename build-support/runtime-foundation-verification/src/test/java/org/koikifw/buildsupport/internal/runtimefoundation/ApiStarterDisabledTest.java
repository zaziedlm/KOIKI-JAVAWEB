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
        properties = "koiki.api.enabled=false")
class ApiStarterDisabledTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private JacksonProperties jacksonProperties;

    @Test
    void backsOffAllKoikiApiDefaultsAndResilienceActivation() {
        assertThat(applicationContext.getBeansOfType(RetryAnnotationBeanPostProcessor.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(ResponseEntityExceptionHandler.class)).isEmpty();
        assertThat(jacksonProperties.isFindAndAddModules()).isTrue();
        assertThat(applicationContext.getEnvironment().getProperty("spring.jackson.find-and-add-modules"))
                .isNull();
        assertThat(applicationContext.getEnvironment().getProperty("spring.mvc.apiversion.use.path-segment"))
                .isNull();
    }
}
