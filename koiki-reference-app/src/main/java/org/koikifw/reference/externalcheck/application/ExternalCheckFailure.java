package org.koikifw.reference.externalcheck.application;

/** Safe Reference failure categories; never includes provider payload or credentials. */
public class ExternalCheckFailure extends RuntimeException {
    public enum Kind { INVALID_INPUT, PROVIDER_REJECTED, INVALID_PROVIDER_RESPONSE,
        PROVIDER_UNAVAILABLE, PROVIDER_RATE_LIMITED, PROVIDER_TIMEOUT, INTERRUPTED, INVALID_CONTEXT }
    private final Kind kind;

    public ExternalCheckFailure(Kind kind) {
        super(kind.name());
        this.kind = kind;
    }

    public Kind kind() { return kind; }

    /** Only a provider's explicit 503 response is eligible for this Reference retry. */
    public static final class TransientRateFailure extends ExternalCheckFailure {
        public TransientRateFailure() { super(Kind.PROVIDER_UNAVAILABLE); }
    }
}
