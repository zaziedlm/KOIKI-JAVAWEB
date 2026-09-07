package org.koikifw.identity;

import java.util.Objects;
import java.util.UUID;

/** Immutable and opaque identifier for a framework user. */
public final class FrameworkUserId {

    private final UUID value;

    private FrameworkUserId(UUID value) {
        this.value = value;
    }

    /** Parses the canonical UUID representation of a framework user ID. */
    public static FrameworkUserId parse(String value) {
        Objects.requireNonNull(value, "value");
        return new FrameworkUserId(UUID.fromString(value));
    }

    /** Returns the database-neutral UUID value. */
    public UUID value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other instanceof FrameworkUserId otherId && value.equals(otherId.value));
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
