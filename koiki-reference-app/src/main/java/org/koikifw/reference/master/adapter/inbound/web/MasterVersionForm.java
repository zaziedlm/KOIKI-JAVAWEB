package org.koikifw.reference.master.adapter.inbound.web;

import jakarta.validation.constraints.PositiveOrZero;
import org.jspecify.annotations.Nullable;

/** Inbound optimistic version for a master state change. */
record MasterVersionForm(@Nullable @PositiveOrZero Long expectedVersion) {

    boolean valid() {
        return expectedVersion != null && expectedVersion >= 0;
    }

    long requiredVersion() {
        return java.util.Objects.requireNonNull(expectedVersion);
    }
}
