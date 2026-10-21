package org.koikifw.reference.expense.adapter.inbound.sessionapi;

import java.util.List;

record SessionExpensePageResponse(
        List<SessionExpenseListItemResponse> content, String totalElements, int page, int size) {}
