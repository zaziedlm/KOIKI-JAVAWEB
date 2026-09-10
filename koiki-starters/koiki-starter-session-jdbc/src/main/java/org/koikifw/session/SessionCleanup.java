package org.koikifw.session;

/** Removes expired KOIKI sessions under the Framework single-execution boundary. */
public interface SessionCleanup {

    /**
     * Removes expired sessions when this process acquires the cleanup boundary.
     *
     * @return the completed or contended outcome
     * @throws SessionCleanupException when lock handling or cleanup fails
     */
    SessionCleanupResult cleanUpExpiredSessions();
}
