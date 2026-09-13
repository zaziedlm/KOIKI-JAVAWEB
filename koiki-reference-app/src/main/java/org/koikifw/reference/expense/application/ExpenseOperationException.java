package org.koikifw.reference.expense.application;

/** Application-boundary rejection without sensitive submitted values. */
public final class ExpenseOperationException extends RuntimeException {

    private final ExpenseFailure failure;

    ExpenseOperationException(ExpenseFailure failure) {
        super("expense operation failed: " + failure);
        this.failure = failure;
    }

    public ExpenseFailure failure() {
        return failure;
    }
}

