package org.koikifw.reference.expense.application.query;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Fully materialized expense detail for one already-scoped view. */
public record ExpenseRequestDetail(
        UUID expenseRequestId,
        UUID applicantUserId,
        String applicantEmail,
        UUID departmentId,
        String departmentCode,
        String departmentName,
        long claimedAmount,
        String status,
        @Nullable String decisionReason,
        long version,
        Instant updatedAt,
        List<ExpenseRequestLineView> lines) {

    public ExpenseRequestDetail {
        Objects.requireNonNull(expenseRequestId, "expenseRequestId");
        Objects.requireNonNull(applicantUserId, "applicantUserId");
        Objects.requireNonNull(applicantEmail, "applicantEmail");
        Objects.requireNonNull(departmentId, "departmentId");
        Objects.requireNonNull(departmentCode, "departmentCode");
        Objects.requireNonNull(departmentName, "departmentName");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(updatedAt, "updatedAt");
        lines = List.copyOf(lines);
    }
}
