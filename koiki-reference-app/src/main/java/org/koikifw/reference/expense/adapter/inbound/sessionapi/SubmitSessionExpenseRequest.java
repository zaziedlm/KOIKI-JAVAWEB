package org.koikifw.reference.expense.adapter.inbound.sessionapi;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import tools.jackson.databind.annotation.JsonDeserialize;

record SubmitSessionExpenseRequest(
        @NotNull @Pattern(regexp = "0|[1-9][0-9]{0,18}") @DecimalMax("9223372036854775807")
        @JsonDeserialize(using = StrictStringDeserializer.class) String expectedVersion) {}
