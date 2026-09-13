package org.koikifw.reference.expense.domain.model;

/** Rejects an operation without exposing submitted values in its message. */
public final class ExpenseDomainException extends RuntimeException {

    private final ExpenseRuleViolation violation;

    ExpenseDomainException(ExpenseRuleViolation violation) {
        super("expense operation rejected: " + violation);
        this.violation = violation;
    }

    public ExpenseRuleViolation violation() {
        return violation;
    }
}

