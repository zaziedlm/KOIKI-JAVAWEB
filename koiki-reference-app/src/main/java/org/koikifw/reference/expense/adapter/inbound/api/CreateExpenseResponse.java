package org.koikifw.reference.expense.adapter.inbound.api;

import java.util.UUID;

/** JSON response identifying a newly created expense request. */
record CreateExpenseResponse(UUID expenseRequestId) {}
