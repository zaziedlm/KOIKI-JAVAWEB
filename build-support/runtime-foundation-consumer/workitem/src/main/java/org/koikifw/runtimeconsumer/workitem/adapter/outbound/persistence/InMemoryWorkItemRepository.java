package org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** CP1-only adapter that keeps the business module executable before database integration. */
public final class InMemoryWorkItemRepository implements WorkItemRepository {

    private final Map<UUID, WorkItem> items = new ConcurrentHashMap<>();

    @Override
    public Optional<WorkItem> findById(UUID id) {
        return Optional.ofNullable(items.get(id));
    }

    @Override
    public WorkItem save(WorkItem workItem) {
        items.put(workItem.getId(), workItem);
        return workItem;
    }
}
