package org.koikifw.reference.expense.adapter.inbound.sessionapi;

import java.time.LocalDate;
import java.util.UUID;

record SessionExpenseLineResponse(UUID expenseLineId, SessionExpenseOptionResponse expenseCategory,
        LocalDate usageDate, String description, String purpose, String amount) {}
