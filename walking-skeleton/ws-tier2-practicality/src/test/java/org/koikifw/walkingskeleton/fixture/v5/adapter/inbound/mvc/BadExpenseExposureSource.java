package org.koikifw.walkingskeleton.fixture.v5.adapter.inbound.mvc;

import org.koikifw.walkingskeleton.tier2.expense.domain.model.ExpenseRequest;

public class BadExpenseExposureSource {

    private final ExpenseRequest expenseRequest;

    public BadExpenseExposureSource(ExpenseRequest expenseRequest) {
        this.expenseRequest = expenseRequest;
    }

    public ExpenseRequest load() {
        return expenseRequest;
    }
}
