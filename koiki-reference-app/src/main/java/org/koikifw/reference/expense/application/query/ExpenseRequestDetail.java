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

    public ExpenseRequestDetail(
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
        this.expenseRequestId = Objects.requireNonNull(expenseRequestId, "expenseRequestId");
        this.applicantUserId = Objects.requireNonNull(applicantUserId, "applicantUserId");
        this.applicantEmail = Objects.requireNonNull(applicantEmail, "applicantEmail");
        this.departmentId = Objects.requireNonNull(departmentId, "departmentId");
        this.departmentCode = Objects.requireNonNull(departmentCode, "departmentCode");
        this.departmentName = Objects.requireNonNull(departmentName, "departmentName");
        this.claimedAmount = claimedAmount;
        this.status = Objects.requireNonNull(status, "status");
        this.decisionReason = decisionReason;
        this.version = version;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.lines = List.copyOf(lines);
    }
}
