package org.koikifw.validation.nullsafety;

/**
 * Minimal source used to prove that a null-marked production package compiles.
 */
public final class NullSafetyProbe {

    private NullSafetyProbe() {
    }

    public static String normalize(String value) {
        return value.trim();
    }
}
