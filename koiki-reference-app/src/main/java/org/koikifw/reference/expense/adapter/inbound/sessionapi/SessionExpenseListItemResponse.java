package org.koikifw.reference.expense.adapter.inbound.sessionapi;

import java.time.Instant;
import java.util.UUID;

record SessionExpenseListItemResponse(UUID expenseRequestId, SessionExpenseOptionResponse department,
        String claimedAmount, String status, String version, Instant updatedAt) {}
