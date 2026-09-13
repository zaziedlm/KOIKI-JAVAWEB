package org.koikifw.reference.master.adapter.inbound.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/** Inbound values for creating a Reference master entry. */
record MasterCreateForm(
        @Nullable @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{0,99}$") String code,
        @Nullable @NotBlank @Size(max = 200) String name) {

    boolean valid() {
        return code != null && !code.isBlank() && name != null && !name.isBlank();
    }

    String requiredCode() {
        return java.util.Objects.requireNonNull(code);
    }

    String requiredName() {
        return java.util.Objects.requireNonNull(name);
    }
}
