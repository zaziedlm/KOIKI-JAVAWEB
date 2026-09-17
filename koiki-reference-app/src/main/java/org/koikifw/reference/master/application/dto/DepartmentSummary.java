package org.koikifw.reference.master.application.dto;

import java.util.Objects;
import java.util.UUID;

/** Materialized department values used by the master list view boundary. */
public record DepartmentSummary(
        UUID departmentId,
        String departmentCode,
        String departmentName,
        boolean active,
        long version) {

    public DepartmentSummary {
        Objects.requireNonNull(departmentId, "departmentId");
        Objects.requireNonNull(departmentCode, "departmentCode");
        Objects.requireNonNull(departmentName, "departmentName");
    }
}
