package org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Spring Data repository used directly by the Tier 1 Application Use Case. */
public interface WorkItemRepository extends Repository<WorkItem, UUID> {

    Optional<WorkItem> findById(UUID id);

    WorkItem save(WorkItem workItem);
}
