package org.koikifw.buildsupport.internal.runtimefoundation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@SpringBootTest(
        classes = ApiStarterSmokeTest.TestApplication.class,
        properties = "koiki.api.problem-details.enabled=false")
class ApiProblemDetailsDisabledTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void disablesOnlyKoikiProblemDetailsHandler() {
        assertThat(applicationContext.getBeansOfType(ResponseEntityExceptionHandler.class)).isEmpty();
    }
}
