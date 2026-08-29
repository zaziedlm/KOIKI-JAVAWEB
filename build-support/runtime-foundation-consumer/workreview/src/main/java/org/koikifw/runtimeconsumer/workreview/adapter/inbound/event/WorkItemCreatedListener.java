package org.koikifw.runtimeconsumer.workreview.adapter.inbound.event;

import java.util.Objects;
import org.koikifw.runtimeconsumer.workitem.domain.event.WorkItemCreated;
import org.koikifw.runtimeconsumer.workreview.application.usecase.RecordWorkReviewUseCase;
import org.springframework.context.event.EventListener;

/** Thin synchronous adapter for the workitem collaboration event. */
public class WorkItemCreatedListener {

    private final RecordWorkReviewUseCase useCase;

    public WorkItemCreatedListener(RecordWorkReviewUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
    }

    @EventListener
    public void on(WorkItemCreated event) {
        useCase.record(event.workItemId(), event.label());
    }
}
