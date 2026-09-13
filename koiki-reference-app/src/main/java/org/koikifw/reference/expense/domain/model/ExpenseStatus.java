package org.koikifw.reference.expense.domain.model;

import java.util.Set;

/** Lifecycle states accepted for a Phase 3 expense request. */
public enum ExpenseStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED,
    RETURNED,
    SETTLED;

    private static final Set<ExpenseStatus> PENDING =
            Set.of(DRAFT, SUBMITTED, RETURNED, APPROVED);

    public static Set<ExpenseStatus> pendingStatuses() {
        return PENDING;
    }
}
