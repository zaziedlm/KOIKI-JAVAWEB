package org.koikifw.reference.expense.adapter.inbound.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/** JSON input for one expense line. */
record CreateExpenseLineRequest(
        @NotNull UUID expenseCategoryId,
        @NotNull @PastOrPresent LocalDate usageDate,
        @NotBlank @Size(max = 200) String description,
        @NotBlank @Size(max = 500) String purpose,
        @NotNull @Positive Long amount) {}
