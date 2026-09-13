package org.koikifw.reference.expense.application.query;

import java.util.List;

/** Stable application paging result for expense request list rows. */
public record ExpenseRequestPage(
        List<ExpenseRequestListItem> content,
        long totalElements,
        int page,
        int size) {

    public ExpenseRequestPage {
        content = List.copyOf(content);
    }
}
