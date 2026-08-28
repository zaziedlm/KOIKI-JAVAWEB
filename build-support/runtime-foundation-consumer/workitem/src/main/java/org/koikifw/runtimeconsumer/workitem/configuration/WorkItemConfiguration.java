package org.koikifw.runtimeconsumer.workitem.configuration;

import org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence.InMemoryWorkItemRepository;
import org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence.WorkItemRepository;
import org.koikifw.runtimeconsumer.workitem.application.usecase.CreateWorkItemUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Consumer-owned assembly for the CP1 business-like module. */
@Configuration(proxyBeanMethods = false)
public class WorkItemConfiguration {

    @Bean
    WorkItemRepository workItemRepository() {
        return new InMemoryWorkItemRepository();
    }

    @Bean
    CreateWorkItemUseCase createWorkItemUseCase(WorkItemRepository repository) {
        return new CreateWorkItemUseCase(repository);
    }
}
