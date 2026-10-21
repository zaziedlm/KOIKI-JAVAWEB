package org.koikifw.reference.expense.adapter.inbound.sessionapi;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import tools.jackson.databind.annotation.JsonDeserialize;

record CreateSessionExpenseLineRequest(
        @NotNull @JsonDeserialize(using = CanonicalUuidDeserializer.class) UUID expenseCategoryId,
        @NotNull @PastOrPresent LocalDate usageDate,
        @NotBlank @Size(max = 200) String description,
        @NotBlank @Size(max = 500) String purpose,
        @NotNull @Pattern(regexp = "[1-9][0-9]{0,18}") @DecimalMax("9223372036854775807")
        @JsonDeserialize(using = StrictStringDeserializer.class) String amount) {}
