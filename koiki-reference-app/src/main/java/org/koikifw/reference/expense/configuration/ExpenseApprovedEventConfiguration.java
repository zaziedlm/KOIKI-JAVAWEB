package org.koikifw.reference.expense.configuration;

import org.koikifw.reference.expense.adapter.outbound.event.SpringExpenseApprovedEventAdapter;
import org.koikifw.reference.expense.application.port.outbound.ExpenseApprovedEventPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class ExpenseApprovedEventConfiguration {
    @Bean
    ExpenseApprovedEventPort expenseApprovedEventPort(
            Environment environment, ApplicationEventPublisher publisher) {
        if ("true".equals(environment.getProperty("koiki.reference.notification.async.enabled", "false"))) {
            return new SpringExpenseApprovedEventAdapter(publisher);
        }
        return event -> { };
    }
}
