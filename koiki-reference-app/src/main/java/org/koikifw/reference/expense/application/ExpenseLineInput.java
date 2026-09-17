package org.koikifw.reference.expense.application;

import java.time.LocalDate;
import java.util.UUID;

/** Technology-neutral input for one complete expense line. */
public record ExpenseLineInput(
        UUID expenseLineId,
        UUID expenseCategoryId,
        LocalDate usageDate,
        String description,
        String purpose,
        long amount) {
}

