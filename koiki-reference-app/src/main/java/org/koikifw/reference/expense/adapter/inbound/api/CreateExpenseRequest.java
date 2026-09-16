package org.koikifw.reference.expense.adapter.inbound.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.UUID;

/** JSON input for creating a draft expense request. */
record CreateExpenseRequest(
        @NotNull UUID departmentId,
        @NotNull @Positive Long claimedAmount,
        @NotEmpty List<@NotNull @Valid CreateExpenseLineRequest> lines) {}
