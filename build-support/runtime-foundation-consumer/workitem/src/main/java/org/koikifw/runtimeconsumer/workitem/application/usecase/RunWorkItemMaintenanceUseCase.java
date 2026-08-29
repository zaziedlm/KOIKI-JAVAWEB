package org.koikifw.runtimeconsumer.workitem.application.usecase;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.koikifw.runtimeconsumer.workitem.application.port.outbound.WorkItemExecutionLock;
import org.koikifw.runtimeconsumer.workitem.application.port.outbound.WorkItemExecutionLock.LockHandle;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

/** Coordinates single execution without extending the business transaction across the task. */
public class RunWorkItemMaintenanceUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunWorkItemMaintenanceUseCase.class);

    private final WorkItemExecutionLock executionLock;
    private final ExecuteWorkItemMaintenanceUseCase executeUseCase;

    public RunWorkItemMaintenanceUseCase(
            WorkItemExecutionLock executionLock,
            ExecuteWorkItemMaintenanceUseCase executeUseCase) {
        this.executionLock = executionLock;
        this.executeUseCase = executeUseCase;
    }

    public WorkItemMaintenanceResult run(WorkItemMaintenanceTask task) {
        UUID executionId = UUID.randomUUID();
        Instant startedAt = Instant.now();
        logLifecycle("started", task, executionId, null);

        try {
            Optional<LockHandle> acquired = executionLock.tryAcquire(
                    task.lockNamespace(), task.lockId());
            if (acquired.isEmpty()) {
                logLifecycle("contended", task, executionId, startedAt);
                return new WorkItemMaintenanceResult(
                        WorkItemMaintenanceResult.Outcome.CONTENDED, executionId);
            }

            logLifecycle("acquired", task, executionId, null);
            try (LockHandle ignored = acquired.orElseThrow()) {
                executeUseCase.execute(task, executionId);
            }
            logLifecycle("succeeded", task, executionId, startedAt);
            return new WorkItemMaintenanceResult(
                    WorkItemMaintenanceResult.Outcome.SUCCEEDED, executionId);
        } catch (RuntimeException exception) {
            LOGGER.atError()
                    .addKeyValue("operation", "workItemMaintenance")
                    .addKeyValue("result", "failed")
                    .addKeyValue("taskKey", task.externalKey())
                    .addKeyValue("executionId", executionId)
                    .addKeyValue("lockOwner", executionId)
                    .addKeyValue("elapsed", Duration.between(startedAt, Instant.now()).toMillis())
                    .addKeyValue("errorCode", "MAINTENANCE_EXECUTION_FAILED")
                    .log("work item maintenance failed");
            return new WorkItemMaintenanceResult(
                    WorkItemMaintenanceResult.Outcome.FAILED, executionId);
        }
    }

    private void logLifecycle(
            String result,
            WorkItemMaintenanceTask task,
            UUID executionId,
            @Nullable Instant startedAt) {
        LoggingEventBuilder event = LOGGER.atInfo()
                .addKeyValue("operation", "workItemMaintenance")
                .addKeyValue("result", result)
                .addKeyValue("taskKey", task.externalKey())
                .addKeyValue("executionId", executionId)
                .addKeyValue("lockOwner", executionId);
        if (startedAt != null) {
            event = event.addKeyValue(
                    "elapsed", Duration.between(startedAt, Instant.now()).toMillis());
        }
        event.log("work item maintenance lifecycle");
    }
}
