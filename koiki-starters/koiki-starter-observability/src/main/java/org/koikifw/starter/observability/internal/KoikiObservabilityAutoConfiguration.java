package org.koikifw.starter.observability.internal;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.context.integration.Slf4jThreadLocalAccessor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.web.filter.OncePerRequestFilter;

/** Configures correlation context and its propagation without exposing KOIKI Java API. */
@AutoConfiguration
@ConditionalOnProperty(prefix = "koiki.observability", name = "enabled", matchIfMissing = true)
public class KoikiObservabilityAutoConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 100)
    @ConditionalOnClass({ContextRegistry.class, Slf4jThreadLocalAccessor.class})
    @ConditionalOnProperty(
            prefix = "koiki.observability.context-propagation",
            name = "enabled",
            matchIfMissing = true)
    TaskDecorator koikiContextPropagatingTaskDecorator() {
        ContextRegistry registry = new ContextRegistry()
                .registerThreadLocalAccessor(new Slf4jThreadLocalAccessor("requestId"));
        ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder()
                .contextRegistry(registry)
                .build();
        return new ContextPropagatingTaskDecorator(snapshotFactory);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass({HttpServletRequest.class, OncePerRequestFilter.class})
    @ConditionalOnProperty(
            prefix = "koiki.observability.correlation",
            name = "enabled",
            matchIfMissing = true)
    @ConditionalOnMissingBean(name = "koikiCorrelationFilter")
    KoikiCorrelationFilter koikiCorrelationFilter() {
        return new KoikiCorrelationFilter();
    }
}
