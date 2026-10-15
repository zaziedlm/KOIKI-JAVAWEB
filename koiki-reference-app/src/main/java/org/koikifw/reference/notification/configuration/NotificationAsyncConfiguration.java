package org.koikifw.reference.notification.configuration;

import java.net.URI;
import org.koikifw.reference.notification.adapter.inbound.event.ExpenseApprovalListener;
import org.koikifw.reference.notification.adapter.outbound.external.HttpNotificationProviderAdapter;
import org.koikifw.reference.notification.application.NotificationDispatchService;
import org.koikifw.reference.notification.application.port.outbound.NotificationProviderPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "koiki.reference.notification.async.enabled", havingValue = "true")
@EnableAsync
public class NotificationAsyncConfiguration {
    @Bean(name = {"applicationTaskExecutor", "taskExecutor"})
    ThreadPoolTaskExecutor notificationExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(16);
        executor.setThreadNamePrefix("reference-notification-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    @Bean
    NotificationProviderPort notificationProvider(Environment environment) {
        return new HttpNotificationProviderAdapter(URI.create(environment.getRequiredProperty(
                "koiki.reference.notification.async.provider-endpoint")));
    }

    @Bean
    NotificationDispatchService notificationDispatchService(NotificationProviderPort provider) {
        return new NotificationDispatchService(provider);
    }

    @Bean
    ExpenseApprovalListener expenseApprovalListener(NotificationDispatchService service) {
        return new ExpenseApprovalListener(service);
    }
}
