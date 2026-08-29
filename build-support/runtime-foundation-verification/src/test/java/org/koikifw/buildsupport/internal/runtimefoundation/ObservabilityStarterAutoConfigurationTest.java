package org.koikifw.buildsupport.internal.runtimefoundation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.Filter;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.koikifw.starter.observability.internal.KoikiObservabilityAutoConfiguration;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.core.task.TaskDecorator;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ObservabilityStarterAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KoikiObservabilityAutoConfiguration.class));

    private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KoikiObservabilityAutoConfiguration.class));

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void propagatesAndClearsMdcAcrossReusedExecutorThread() {
        contextRunner.run(context -> {
            TaskDecorator decorator = context.getBean("koikiContextPropagatingTaskDecorator", TaskDecorator.class);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                AtomicReference<@Nullable String> firstTaskRequestId = new AtomicReference<>();
                MDC.put("requestId", "request-a");
                Runnable firstTask = decorator.decorate(
                        () -> firstTaskRequestId.set(MDC.get("requestId")));
                MDC.clear();
                executor.submit(firstTask).get(10, TimeUnit.SECONDS);

                AtomicReference<@Nullable String> secondTaskRequestId = new AtomicReference<>();
                Runnable secondTask = decorator.decorate(
                        () -> secondTaskRequestId.set(MDC.get("requestId")));
                executor.submit(secondTask).get(10, TimeUnit.SECONDS);

                assertThat(firstTaskRequestId).hasValue("request-a");
                assertThat(secondTaskRequestId).hasNullValue();
            } finally {
                executor.shutdownNow();
            }
        });
    }

    @Test
    void coexistsWithApplicationOwnedTaskDecorator() {
        TaskDecorator applicationDecorator = runnable -> runnable;

        contextRunner
                .withBean("applicationTaskDecorator", TaskDecorator.class, () -> applicationDecorator)
                .run(context -> assertThat(context.getBeansOfType(TaskDecorator.class))
                        .containsKeys("koikiContextPropagatingTaskDecorator", "applicationTaskDecorator")
                        .hasSize(2));
    }

    @Test
    void acceptsSafeRequestIdAndCleansRequestThreadMdc() {
        webContextRunner.run(context -> {
            Filter filter = context.getBean("koikiCorrelationFilter", Filter.class);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Request-ID", "customer-request-01");
            MockHttpServletResponse response = new MockHttpServletResponse();
            AtomicReference<@Nullable String> observed = new AtomicReference<>();

            filter.doFilter(request, response, (servletRequest, servletResponse) ->
                    observed.set(MDC.get("requestId")));

            assertThat(observed).hasValue("customer-request-01");
            assertThat(response.getHeader("X-Request-ID")).isEqualTo("customer-request-01");
            assertThat(MDC.get("requestId")).isNull();
        });
    }

    @Test
    void replacesUnsafeRequestIdInsteadOfReflectingIt() {
        webContextRunner.run(context -> {
            Filter filter = context.getBean("koikiCorrelationFilter", Filter.class);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Request-ID", "unsafe\nvalue");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            });

            assertThat(response.getHeader("X-Request-ID"))
                    .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
                    .doesNotContain("unsafe");
        });
    }

    @Test
    void disablesIndividualPartsOrTheWholeStarter() {
        contextRunner
                .withPropertyValues("koiki.observability.context-propagation.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(TaskDecorator.class));
        webContextRunner
                .withPropertyValues("koiki.observability.correlation.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(Filter.class));
        webContextRunner
                .withPropertyValues("koiki.observability.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(Filter.class);
                    assertThat(context).doesNotHaveBean(TaskDecorator.class);
                });
    }
}
