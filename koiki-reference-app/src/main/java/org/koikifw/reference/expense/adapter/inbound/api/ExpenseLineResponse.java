package org.koikifw.reference.expense.adapter.inbound.api;

import java.time.LocalDate;
import java.util.UUID;

/** JSON representation of one expense line. */
record ExpenseLineResponse(
        UUID expenseLineId,
        UUID expenseCategoryId,
        String expenseCategoryCode,
        String expenseCategoryName,
        LocalDate usageDate,
        String description,
        String purpose,
        long amount) {}
