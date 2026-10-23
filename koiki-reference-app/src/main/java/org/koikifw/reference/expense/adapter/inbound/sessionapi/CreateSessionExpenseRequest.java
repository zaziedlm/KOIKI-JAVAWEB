package org.koikifw.reference.expense.adapter.inbound.sessionapi;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.annotation.JsonDeserialize;

record CreateSessionExpenseRequest(
        @NotNull @JsonDeserialize(using = CanonicalUuidDeserializer.class) UUID departmentId,
        @NotNull @Pattern(regexp = "[1-9][0-9]{0,18}") @DecimalMax("9223372036854775807")
        @JsonDeserialize(using = StrictStringDeserializer.class) String claimedAmount,
        @NotEmpty List<@NotNull @Valid CreateSessionExpenseLineRequest> lines) {}
