package org.koikifw.runtimeconsumer.workitem.application.usecase;

import java.util.Arrays;
import java.util.Optional;

/** Customer-like task keys registered by the workitem feature. */
public enum WorkItemMaintenanceTask {
    PRIMARY("workitem-maintenance-primary", 1),
    SECONDARY("workitem-maintenance-secondary", 2);

    private static final int LOCK_NAMESPACE = 1_263_487_307;

    private final String externalKey;
    private final int lockId;

    WorkItemMaintenanceTask(String externalKey, int lockId) {
        this.externalKey = externalKey;
        this.lockId = lockId;
    }

    public String externalKey() {
        return externalKey;
    }

    public int lockNamespace() {
        return LOCK_NAMESPACE;
    }

    public int lockId() {
        return lockId;
    }

    public static Optional<WorkItemMaintenanceTask> fromExternalKey(String externalKey) {
        return Arrays.stream(values())
                .filter(task -> task.externalKey.equals(externalKey))
                .findFirst();
    }
}
