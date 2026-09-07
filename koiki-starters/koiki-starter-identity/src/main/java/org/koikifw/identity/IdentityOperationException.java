package org.koikifw.identity;

import java.util.Objects;

/** Identity operation failure that does not expose identifiers or persistence details. */
public final class IdentityOperationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final IdentityFailure failure;

    public IdentityOperationException(IdentityFailure failure) {
        super("Identity operation failed.");
        this.failure = Objects.requireNonNull(failure, "failure");
    }

    public IdentityFailure failure() {
        return failure;
    }
}
