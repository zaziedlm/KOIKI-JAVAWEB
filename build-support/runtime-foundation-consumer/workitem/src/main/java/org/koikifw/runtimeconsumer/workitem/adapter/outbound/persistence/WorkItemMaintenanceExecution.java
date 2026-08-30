package org.koikifw.runtimeconsumer.workitem.adapter.outbound.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Behavior-free persistence model for externally observable maintenance executions. */
@Entity
@Table(name = "kkbiz_work_item_maintenance")
public class WorkItemMaintenanceExecution {

    @Id
    private String taskKey = "";

    private long executionCount;

    private @Nullable UUID lastExecutionId;

    private @Nullable Instant lastExecutedAt;

    protected WorkItemMaintenanceExecution() {
    }

    public WorkItemMaintenanceExecution(String taskKey) {
        this.taskKey = Objects.requireNonNull(taskKey, "taskKey");
    }

    public void record(UUID executionId, Instant executedAt) {
        executionCount++;
        lastExecutionId = Objects.requireNonNull(executionId, "executionId");
        lastExecutedAt = Objects.requireNonNull(executedAt, "executedAt");
    }

    public String getTaskKey() {
        return taskKey;
    }

    public long getExecutionCount() {
        return executionCount;
    }

    public @Nullable UUID getLastExecutionId() {
        return lastExecutionId;
    }

    public @Nullable Instant getLastExecutedAt() {
        return lastExecutedAt;
    }
}
