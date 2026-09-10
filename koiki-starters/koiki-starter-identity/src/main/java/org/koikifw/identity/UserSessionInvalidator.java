package org.koikifw.identity;

/** Synchronously invalidates every KOIKI session for one framework user. */
public interface UserSessionInvalidator {

    void invalidateAll(FrameworkUserId userId);
}
