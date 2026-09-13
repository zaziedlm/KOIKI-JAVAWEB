package org.koikifw.reference.master.adapter.inbound.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/** Inbound values for one optimistic master rename. */
record MasterRenameForm(
        @Nullable @NotBlank @Size(max = 200) String name,
        @Nullable @PositiveOrZero Long expectedVersion) {

    boolean valid() {
        return name != null && !name.isBlank() && expectedVersion != null && expectedVersion >= 0;
    }

    String requiredName() {
        return java.util.Objects.requireNonNull(name);
    }

    long requiredVersion() {
        return java.util.Objects.requireNonNull(expectedVersion);
    }
}
