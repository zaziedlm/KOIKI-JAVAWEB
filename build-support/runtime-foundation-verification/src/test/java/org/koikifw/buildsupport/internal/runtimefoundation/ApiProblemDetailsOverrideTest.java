package org.koikifw.buildsupport.internal.runtimefoundation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@SpringBootTest(classes = ApiProblemDetailsOverrideTest.OverrideApplication.class)
class ApiProblemDetailsOverrideTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void applicationOwnedHandlerReplacesKoikiDefault() {
        assertThat(applicationContext.getBeansOfType(ResponseEntityExceptionHandler.class))
                .containsOnlyKeys("applicationProblemDetailsHandler")
                .allSatisfy((name, handler) -> assertThat(handler)
                        .isInstanceOf(ApplicationProblemDetailsHandler.class));
    }

    @RestControllerAdvice
    static final class ApplicationProblemDetailsHandler extends ResponseEntityExceptionHandler {
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class OverrideApplication {

        @Bean
        ResponseEntityExceptionHandler applicationProblemDetailsHandler() {
            return new ApplicationProblemDetailsHandler();
        }
    }
}
