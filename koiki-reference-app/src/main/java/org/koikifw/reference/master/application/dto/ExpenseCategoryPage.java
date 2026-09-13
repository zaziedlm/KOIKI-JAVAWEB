package org.koikifw.reference.master.application.dto;

import java.util.List;

/** Stable application paging result for expense-category summaries. */
public record ExpenseCategoryPage(
        List<ExpenseCategorySummary> content,
        long totalElements,
        int page,
        int size) {

    public ExpenseCategoryPage {
        content = List.copyOf(content);
    }
}
