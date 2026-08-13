package org.koikifw.walkingskeleton.tier2.expense.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ExpenseDetailView(
        UUID expenseRequestId,
        String description,
        BigDecimal requestedAmount,
        String status,
        List<ExpenseLineView> lines) {

    public ExpenseDetailView {
        lines = List.copyOf(lines);
    }

    public record ExpenseLineView(
            UUID expenseLineId,
            String description,
            BigDecimal amount) {
    }
}
