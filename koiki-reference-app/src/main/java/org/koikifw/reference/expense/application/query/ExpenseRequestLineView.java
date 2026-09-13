package org.koikifw.reference.expense.application.query;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Display-only expense line with its category label materialized. */
public record ExpenseRequestLineView(
        UUID expenseLineId,
        UUID expenseCategoryId,
        String expenseCategoryCode,
        String expenseCategoryName,
        LocalDate usageDate,
        String description,
        String purpose,
        long amount) {

    public ExpenseRequestLineView {
        Objects.requireNonNull(expenseLineId, "expenseLineId");
        Objects.requireNonNull(expenseCategoryId, "expenseCategoryId");
        Objects.requireNonNull(expenseCategoryCode, "expenseCategoryCode");
        Objects.requireNonNull(expenseCategoryName, "expenseCategoryName");
        Objects.requireNonNull(usageDate, "usageDate");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(purpose, "purpose");
    }
}
