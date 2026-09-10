package org.koikifw.identity.internal;

import java.util.Arrays;

final class SourceFingerprint {

    private final String keyId;
    private final byte[] value;

    SourceFingerprint(String keyId, byte[] value) {
        this.keyId = keyId;
        this.value = value.clone();
    }

    String keyId() {
        return keyId;
    }

    byte[] value() {
        return value.clone();
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other instanceof SourceFingerprint that
                        && keyId.equals(that.keyId)
                        && Arrays.equals(value, that.value));
    }

    @Override
    public int hashCode() {
        return 31 * keyId.hashCode() + Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        return "SourceFingerprint[redacted]";
    }
}
