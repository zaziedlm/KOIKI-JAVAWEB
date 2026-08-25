package org.koikifw.validation.nullsafety;

/**
 * Deliberate NullAway violation isolated from normal production sources.
 */
public final class NullSafetyProbe {

    private NullSafetyProbe() {
    }

    public static String deliberateViolation() {
        return null;
    }
}
