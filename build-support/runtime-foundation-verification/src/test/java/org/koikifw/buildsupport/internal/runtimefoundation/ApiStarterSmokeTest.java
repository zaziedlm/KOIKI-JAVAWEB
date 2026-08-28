package org.koikifw.buildsupport.internal.runtimefoundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.Validator;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.jackson.autoconfigure.JacksonProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.resilience.annotation.RetryAnnotationBeanPostProcessor;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.web.servlet.DispatcherServlet;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(classes = ApiStarterSmokeTest.TestApplication.class)
class ApiStarterSmokeTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Validator validator;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JacksonProperties jacksonProperties;

    @Autowired
    private RetryProbe retryProbe;

    @BeforeEach
    void resetRetryProbe() {
        retryProbe.reset();
    }

    @Test
    void enablesServletMvcValidationJacksonAndResilienceDefaults() {
        assertThat(applicationContext.getBean(DispatcherServlet.class)).isNotNull();
        assertThat(validator).isNotNull();
        assertThat(jsonMapper).isNotNull();
        assertThat(jacksonProperties.isFindAndAddModules()).isFalse();
        assertThat(applicationContext.getBeansOfType(JsonMapperBuilderCustomizer.class)).isNotEmpty();
        assertThat(applicationContext.getBeansOfType(RetryAnnotationBeanPostProcessor.class)).hasSize(1);
        assertThat(applicationContext.getEnvironment().getProperty("spring.jackson.find-and-add-modules"))
                .isEqualTo("false");
        assertThat(applicationContext.getEnvironment().getProperty("spring.mvc.apiversion.use.path-segment"))
                .isEqualTo("1");
        assertThat(applicationContext.getEnvironment().getProperty("spring.mvc.apiversion.supported"))
                .isEqualTo("1");
    }

    @Test
    void appliesStrictJacksonDefaults() {
        assertThatThrownBy(() -> jsonMapper.readValue(
                "{\"label\":\"accepted\",\"unexpected\":true}", Payload.class))
                .isInstanceOf(tools.jackson.core.JacksonException.class);
    }

    @Test
    void retriesOnlyIncludedFailuresUsingKoikiPlaceholders() {
        assertThat(retryProbe.succeedAfterTransientFailures()).isEqualTo(3);
        assertThat(retryProbe.transientAttempts()).isEqualTo(3);

        assertThatThrownBy(retryProbe::failWithExcludedException)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("not retryable");
        assertThat(retryProbe.excludedAttempts()).isEqualTo(1);
    }

    record Payload(String label) {
    }

    interface RetryProbe {

        int succeedAfterTransientFailures();

        void failWithExcludedException();

        int transientAttempts();

        int excludedAttempts();

        void reset();
    }

    static class DefaultRetryProbe implements RetryProbe {

        private final AtomicInteger transientAttempts = new AtomicInteger();
        private final AtomicInteger excludedAttempts = new AtomicInteger();

        @Override
        @Retryable(
                includes = IllegalStateException.class,
                maxRetriesString = "${koiki.api.resilience.retry.max-retries}",
                delayString = "${koiki.api.resilience.retry.delay}",
                timeoutString = "${koiki.api.resilience.retry.timeout}")
        public int succeedAfterTransientFailures() {
            int attempt = transientAttempts.incrementAndGet();
            if (attempt < 3) {
                throw new IllegalStateException("transient");
            }
            return attempt;
        }

        @Override
        @Retryable(
                includes = IllegalStateException.class,
                maxRetriesString = "${koiki.api.resilience.retry.max-retries}",
                delayString = "${koiki.api.resilience.retry.delay}",
                timeoutString = "${koiki.api.resilience.retry.timeout}")
        public void failWithExcludedException() {
            excludedAttempts.incrementAndGet();
            throw new IllegalArgumentException("not retryable");
        }

        @Override
        public int transientAttempts() {
            return transientAttempts.get();
        }

        @Override
        public int excludedAttempts() {
            return excludedAttempts.get();
        }

        @Override
        public void reset() {
            transientAttempts.set(0);
            excludedAttempts.set(0);
        }
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class TestApplication {

        @Bean
        RetryProbe retryProbe() {
            return new DefaultRetryProbe();
        }
    }
}
