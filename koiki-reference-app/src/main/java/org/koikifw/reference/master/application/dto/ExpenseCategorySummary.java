package org.koikifw.reference.master.application.dto;

import java.util.Objects;
import java.util.UUID;

/** Materialized expense-category values used by the master list view boundary. */
public record ExpenseCategorySummary(
        UUID expenseCategoryId,
        String expenseCategoryCode,
        String expenseCategoryName,
        boolean active,
        long version) {

    public ExpenseCategorySummary {
        Objects.requireNonNull(expenseCategoryId, "expenseCategoryId");
        Objects.requireNonNull(expenseCategoryCode, "expenseCategoryCode");
        Objects.requireNonNull(expenseCategoryName, "expenseCategoryName");
    }
}
