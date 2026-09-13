package org.koikifw.reference.expense.application.query;

import java.util.Objects;
import java.util.UUID;

/** Current display option whose validity is rechecked by the command use case. */
public record ExpenseSelectionOption(UUID id, String code, String name) {

    public ExpenseSelectionOption {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
    }
}
