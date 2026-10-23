package org.koikifw.reference.expense.adapter.inbound.sessionapi;

import java.util.List;

record SessionExpenseOptionsResponse(
        List<SessionExpenseOptionResponse> departments, List<SessionExpenseOptionResponse> expenseCategories) {}
