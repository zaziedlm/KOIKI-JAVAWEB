package org.koikifw.runtimeconsumer.workitem.application.usecase;

import java.time.Instant;
import java.util.UUID;
import org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence.WorkItemMaintenanceExecution;
import org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence.WorkItemMaintenanceRepository;
import org.springframework.transaction.annotation.Transactional;

/** Records the Customer-like maintenance side effect in one business transaction. */
public class ExecuteWorkItemMaintenanceUseCase {

    private final WorkItemMaintenanceRepository repository;

    public ExecuteWorkItemMaintenanceUseCase(WorkItemMaintenanceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void execute(WorkItemMaintenanceTask task, UUID executionId) {
        WorkItemMaintenanceExecution execution = repository.findById(task.externalKey())
                .orElseThrow(() -> new IllegalStateException("maintenance task is not registered"));
        execution.record(executionId, Instant.now());
        repository.save(execution);
    }
}
