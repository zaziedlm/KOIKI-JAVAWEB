package org.koikifw.reference.master.application;

import java.util.Objects;

/** Master operation failure that does not expose identifiers or persistence details. */
public final class MasterOperationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final MasterFailure failure;

    public MasterOperationException(MasterFailure failure) {
        super("Master operation failed.");
        this.failure = Objects.requireNonNull(failure, "failure");
    }

    public MasterFailure failure() {
        return failure;
    }
}
