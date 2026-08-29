package org.koikifw.runtimeconsumer.workitem.application.usecase;

import java.util.Objects;
import java.util.UUID;
import org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence.WorkItem;
import org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence.WorkItemRepository;
import org.koikifw.runtimeconsumer.workitem.domain.event.WorkItemCreated;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates creation and owns the simple Tier 1 business decision. */
public class CreateWorkItemUseCase {

    private final WorkItemRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public CreateWorkItemUseCase(
            WorkItemRepository repository, ApplicationEventPublisher eventPublisher) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
    }

    @Transactional
    public UUID create(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        WorkItem created = new WorkItem(UUID.randomUUID(), label);
        WorkItem saved = repository.save(created);
        eventPublisher.publishEvent(new WorkItemCreated(saved.getId(), saved.getLabel()));
        return saved.getId();
    }
}
