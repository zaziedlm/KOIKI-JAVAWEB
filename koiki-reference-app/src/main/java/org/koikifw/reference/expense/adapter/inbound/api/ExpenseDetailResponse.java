package org.koikifw.reference.expense.adapter.inbound.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Applicant-safe JSON representation of an expense request. */
record ExpenseDetailResponse(
        UUID expenseRequestId,
        ExpenseDepartmentResponse department,
        long claimedAmount,
        String status,
        @Nullable String decisionReason,
        long version,
        Instant updatedAt,
        List<ExpenseLineResponse> lines) {}
