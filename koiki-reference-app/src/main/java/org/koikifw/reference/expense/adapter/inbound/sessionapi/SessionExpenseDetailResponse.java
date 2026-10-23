package org.koikifw.reference.expense.adapter.inbound.sessionapi;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

record SessionExpenseDetailResponse(UUID expenseRequestId, SessionExpenseOptionResponse department,
        String claimedAmount, String status, @Nullable String decisionReason, String version,
        Instant updatedAt, List<SessionExpenseLineResponse> lines) {}
