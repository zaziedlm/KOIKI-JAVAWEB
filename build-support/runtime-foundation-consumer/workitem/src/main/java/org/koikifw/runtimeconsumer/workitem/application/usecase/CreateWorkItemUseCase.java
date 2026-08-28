package org.koikifw.runtimeconsumer.workitem.application.usecase;

import java.util.Objects;
import java.util.UUID;
import org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence.WorkItem;
import org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence.WorkItemRepository;

/** Coordinates creation and owns the simple Tier 1 business decision. */
public final class CreateWorkItemUseCase {

    private final WorkItemRepository repository;

    public CreateWorkItemUseCase(WorkItemRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public UUID create(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        WorkItem created = new WorkItem(UUID.randomUUID(), label);
        return repository.save(created).getId();
    }
}
