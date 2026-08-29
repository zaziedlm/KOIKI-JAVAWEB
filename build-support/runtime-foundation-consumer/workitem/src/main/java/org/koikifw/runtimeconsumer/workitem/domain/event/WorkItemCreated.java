package org.koikifw.runtimeconsumer.workitem.domain.event;

import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

/** Immutable cross-module fact emitted after a work item is stored. */
@NullMarked
public record WorkItemCreated(UUID workItemId, String label) {

    public WorkItemCreated {
        Objects.requireNonNull(workItemId, "workItemId");
        Objects.requireNonNull(label, "label");
    }
}
