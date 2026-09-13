package org.koikifw.reference.master.application.dto;

import java.util.List;

/** Stable application paging result for department summaries. */
public record DepartmentPage(
        List<DepartmentSummary> content,
        long totalElements,
        int page,
        int size) {

    public DepartmentPage {
        content = List.copyOf(content);
    }
}
