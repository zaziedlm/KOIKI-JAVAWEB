package org.koikifw.audit;

/** Safe public failure raised when an audit event cannot be recorded. */
public final class AuditRecordingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Creates a failure without exposing audit payload or persistence detail. */
    public AuditRecordingException() {
        super("Audit recording failed.");
    }
}
