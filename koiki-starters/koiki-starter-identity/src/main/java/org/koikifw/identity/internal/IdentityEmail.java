package org.koikifw.identity.internal;

import java.util.Locale;
import java.util.Objects;

/** Normalized, case-preserving email and its lookup key as one internal value. */
final class IdentityEmail {

    private static final int MAXIMUM_LENGTH = 320;

    private final String value;
    private final String canonicalValue;

    private IdentityEmail(String value, String canonicalValue) {
        this.value = value;
        this.canonicalValue = canonicalValue;
    }

    static IdentityEmail from(String value) {
        Objects.requireNonNull(value, "value");
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > MAXIMUM_LENGTH) {
            throw new IllegalArgumentException("Identity email is invalid.");
        }
        if (normalized.codePoints().anyMatch(codePoint -> codePoint > 0x7f)) {
            throw new IllegalArgumentException("Identity email is invalid.");
        }
        return new IdentityEmail(normalized, normalized.toLowerCase(Locale.ROOT));
    }

    String value() {
        return value;
    }

    String canonicalValue() {
        return canonicalValue;
    }
}
