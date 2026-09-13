package org.koikifw.reference.expense.adapter.inbound.web;

import jakarta.validation.constraints.PositiveOrZero;
import org.jspecify.annotations.Nullable;

/** Inbound optimistic version for one expense transition. */
record ExpenseVersionForm(@Nullable @PositiveOrZero Long expectedVersion) {

    boolean valid() {
        return expectedVersion != null && expectedVersion >= 0;
    }

    long requiredVersion() {
        return java.util.Objects.requireNonNull(expectedVersion);
    }
}
