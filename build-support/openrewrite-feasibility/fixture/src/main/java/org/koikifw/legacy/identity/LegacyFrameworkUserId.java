package org.koikifw.legacy.identity;

import java.util.UUID;

/** Synthetic old API used only by the non-distributed feasibility fixture. */
public record LegacyFrameworkUserId(UUID value) {

    public static LegacyFrameworkUserId parse(String value) {
        return new LegacyFrameworkUserId(UUID.fromString(value));
    }
}
