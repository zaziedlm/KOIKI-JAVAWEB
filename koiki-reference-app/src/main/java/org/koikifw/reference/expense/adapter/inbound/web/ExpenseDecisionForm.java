package org.koikifw.reference.expense.adapter.inbound.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/** Inbound reason and version for reject or return decisions. */
record ExpenseDecisionForm(
        @Nullable @NotBlank @Size(max = 500) String reason,
        @Nullable @PositiveOrZero Long expectedVersion) {

    boolean valid() {
        return reason != null && !reason.isBlank() && expectedVersion != null && expectedVersion >= 0;
    }

    String requiredReason() {
        return java.util.Objects.requireNonNull(reason);
    }

    long requiredVersion() {
        return java.util.Objects.requireNonNull(expectedVersion);
    }
}
