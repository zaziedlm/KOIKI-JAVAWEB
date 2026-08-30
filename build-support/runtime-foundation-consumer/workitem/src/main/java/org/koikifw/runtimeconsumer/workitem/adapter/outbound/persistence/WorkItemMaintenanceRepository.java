package org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence;

import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Spring Data repository for the Customer-like maintenance side effect. */
public interface WorkItemMaintenanceRepository
        extends Repository<WorkItemMaintenanceExecution, String> {

    Optional<WorkItemMaintenanceExecution> findById(String taskKey);

    WorkItemMaintenanceExecution save(WorkItemMaintenanceExecution execution);
}
