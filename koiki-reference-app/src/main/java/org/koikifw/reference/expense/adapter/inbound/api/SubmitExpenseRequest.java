package org.koikifw.reference.expense.adapter.inbound.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** JSON input carrying the client's expected optimistic-lock version. */
record SubmitExpenseRequest(@NotNull @PositiveOrZero Long expectedVersion) {}
