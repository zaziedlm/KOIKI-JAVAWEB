package org.koikifw.reference.expense.application.query;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Fully materialized expense list row; no persistence entity is exposed. */
public record ExpenseRequestListItem(
        UUID expenseRequestId,
        UUID applicantUserId,
        String applicantEmail,
        UUID departmentId,
        String departmentCode,
        String departmentName,
        long claimedAmount,
        String status,
        long version,
        Instant updatedAt) {

    public ExpenseRequestListItem {
        Objects.requireNonNull(expenseRequestId, "expenseRequestId");
        Objects.requireNonNull(applicantUserId, "applicantUserId");
        Objects.requireNonNull(applicantEmail, "applicantEmail");
        Objects.requireNonNull(departmentId, "departmentId");
        Objects.requireNonNull(departmentCode, "departmentCode");
        Objects.requireNonNull(departmentName, "departmentName");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
