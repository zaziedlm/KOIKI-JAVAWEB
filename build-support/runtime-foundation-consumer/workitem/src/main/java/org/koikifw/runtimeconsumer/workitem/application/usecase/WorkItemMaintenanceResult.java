package org.koikifw.runtimeconsumer.workitem.application.usecase;

import java.util.UUID;

/** Result observed by the dedicated process boundary. */
public record WorkItemMaintenanceResult(Outcome outcome, UUID executionId) {

    public enum Outcome {
        SUCCEEDED,
        CONTENDED,
        FAILED
    }
}
