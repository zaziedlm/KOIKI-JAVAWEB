package org.koikifw.buildsupport.internal.runtimefoundation;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

@SpringBootTest(
        classes = ApiStarterSmokeTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiStarterSmokeTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void starterProvidesServletMvcAndValidationRuntime() {
        assertInstanceOf(DispatcherServlet.class, context.getBean("dispatcherServlet"));
        assertNotNull(context.getBean(Validator.class));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
