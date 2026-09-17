package org.koikifw.reference.expense.adapter.inbound.api;

import java.util.UUID;

/** Department representation embedded in an expense detail. */
record ExpenseDepartmentResponse(UUID departmentId, String code, String name) {}
