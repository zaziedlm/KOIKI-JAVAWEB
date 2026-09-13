package org.koikifw.reference.master.domain.event;

import java.util.Objects;
import java.util.UUID;

/** Immutable command-consistency event published before a department is deactivated. */
public record DepartmentDeactivating(UUID departmentId) {

    public DepartmentDeactivating {
        Objects.requireNonNull(departmentId, "departmentId");
    }
}
