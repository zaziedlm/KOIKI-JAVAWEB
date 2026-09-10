package org.koikifw.session;

/** Vendor-neutral result of one expired-session cleanup attempt. */
public enum SessionCleanupResult {
    /** Cleanup acquired the single-execution boundary and completed. */
    COMPLETED,

    /** Another process held the boundary, so cleanup was not invoked. */
    CONTENDED
}
