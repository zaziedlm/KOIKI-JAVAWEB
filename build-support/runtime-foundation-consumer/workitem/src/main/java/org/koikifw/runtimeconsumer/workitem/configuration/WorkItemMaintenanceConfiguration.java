package org.koikifw.runtimeconsumer.workitem.configuration;

import javax.sql.DataSource;
import org.koikifw.runtimeconsumer.workitem.adapter.inbound.command.WorkItemMaintenanceRunner;
import org.koikifw.runtimeconsumer.workitem.adapter.outbound.lock.PostgreSqlWorkItemExecutionLock;
import org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence.WorkItemMaintenanceRepository;
import org.koikifw.runtimeconsumer.workitem.application.port.outbound.WorkItemExecutionLock;
import org.koikifw.runtimeconsumer.workitem.application.usecase.ExecuteWorkItemMaintenanceUseCase;
import org.koikifw.runtimeconsumer.workitem.application.usecase.RunWorkItemMaintenanceUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Assembles maintenance processing only for the dedicated non-web process mode. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "koiki.consumer", name = "mode", havingValue = "maintenance")
public class WorkItemMaintenanceConfiguration {

    @Bean
    WorkItemExecutionLock workItemExecutionLock(DataSource dataSource) {
        return new PostgreSqlWorkItemExecutionLock(dataSource);
    }

    @Bean
    ExecuteWorkItemMaintenanceUseCase executeWorkItemMaintenanceUseCase(
            WorkItemMaintenanceRepository repository) {
        return new ExecuteWorkItemMaintenanceUseCase(repository);
    }

    @Bean
    RunWorkItemMaintenanceUseCase runWorkItemMaintenanceUseCase(
            WorkItemExecutionLock executionLock,
            ExecuteWorkItemMaintenanceUseCase executeUseCase) {
        return new RunWorkItemMaintenanceUseCase(executionLock, executeUseCase);
    }

    @Bean
    WorkItemMaintenanceRunner workItemMaintenanceRunner(RunWorkItemMaintenanceUseCase useCase) {
        return new WorkItemMaintenanceRunner(useCase);
    }
}
