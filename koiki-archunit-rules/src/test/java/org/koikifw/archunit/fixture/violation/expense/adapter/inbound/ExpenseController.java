package org.koikifw.archunit.fixture.violation.expense.adapter.inbound;

import org.koikifw.archunit.fixture.violation.expense.domain.model.Expense;

public class ExpenseController {

    public Expense submit(Expense expense) {
        return expense;
    }
}
