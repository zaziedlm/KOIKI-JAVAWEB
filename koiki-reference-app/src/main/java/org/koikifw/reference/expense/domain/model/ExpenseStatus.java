package org.koikifw.reference.expense.domain.model;

/** Lifecycle states accepted for a Phase 3 expense request. */
public enum ExpenseStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED,
    RETURNED,
    SETTLED
}

