package org.koikifw.reference.expense.application;

/** Safe failure vocabulary for future MVC and REST adapters. */
public enum ExpenseFailure {
    INVALID_INPUT,
    MASTER_UNAVAILABLE,
    NOT_FOUND,
    INVALID_TRANSITION,
    SELF_DECISION,
    CONCURRENT_MODIFICATION,
    CONFLICT,
    DEPENDENCY_FAILURE
}
