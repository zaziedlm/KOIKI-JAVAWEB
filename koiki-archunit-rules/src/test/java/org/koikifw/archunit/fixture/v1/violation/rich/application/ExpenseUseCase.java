package org.koikifw.archunit.fixture.v1.violation.rich.application;

import org.koikifw.archunit.fixture.v1.violation.rich.domain.model.Expense;

public class ExpenseUseCase {
    public Expense load() {
        return new Expense();
    }
}
