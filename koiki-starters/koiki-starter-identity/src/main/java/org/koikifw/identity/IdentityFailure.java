package org.koikifw.identity;

/** Safe failure category for Identity use cases. */
public enum IdentityFailure {
    INVALID_INPUT,
    NOT_FOUND,
    CONFLICT,
    CONCURRENT_MODIFICATION,
    DEPENDENCY_FAILURE
}
