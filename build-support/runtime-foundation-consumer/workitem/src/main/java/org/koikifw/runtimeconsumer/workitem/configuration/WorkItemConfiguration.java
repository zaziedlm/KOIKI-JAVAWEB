package org.koikifw.runtimeconsumer.workitem.configuration;

import org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence.WorkItemRepository;
import org.koikifw.runtimeconsumer.workitem.application.usecase.CreateWorkItemUseCase;
import org.koikifw.runtimeconsumer.workitem.application.usecase.ProcessWorkItemAsyncUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Consumer-owned assembly for the CP1 business-like module. */
@Configuration(proxyBeanMethods = false)
public class WorkItemConfiguration {

    @Bean
    CreateWorkItemUseCase createWorkItemUseCase(WorkItemRepository repository) {
        return new CreateWorkItemUseCase(repository);
    }

    @Bean
    ProcessWorkItemAsyncUseCase processWorkItemAsyncUseCase() {
        return new ProcessWorkItemAsyncUseCase();
    }
}
