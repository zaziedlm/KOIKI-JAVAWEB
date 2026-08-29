package org.koikifw.runtimeconsumer.workreview.configuration;

import org.koikifw.runtimeconsumer.workreview.adapter.inbound.event.WorkItemCreatedListener;
import org.koikifw.runtimeconsumer.workreview.application.usecase.RecordWorkReviewUseCase;
import org.koikifw.runtimeconsumer.workreview.domain.repository.WorkReviewRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Consumer-owned assembly for the Tier 2 collaboration module. */
@Configuration(proxyBeanMethods = false)
public class WorkReviewConfiguration {

    @Bean
    RecordWorkReviewUseCase recordWorkReviewUseCase(WorkReviewRepository repository) {
        return new RecordWorkReviewUseCase(repository);
    }

    @Bean
    WorkItemCreatedListener workItemCreatedListener(RecordWorkReviewUseCase useCase) {
        return new WorkItemCreatedListener(useCase);
    }
}
