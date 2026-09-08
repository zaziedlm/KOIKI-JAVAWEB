package org.koikifw.session;

/** Safe failure reported when expired-session cleanup cannot complete. */
public final class SessionCleanupException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final String SAFE_MESSAGE = "KOIKI expired session cleanup failed";

    /** Creates a cleanup failure without retaining vendor-specific details. */
    public SessionCleanupException() {
        super(SAFE_MESSAGE);
    }
}
