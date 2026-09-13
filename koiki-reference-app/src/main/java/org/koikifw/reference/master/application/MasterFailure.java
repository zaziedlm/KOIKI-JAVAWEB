package org.koikifw.reference.master.application;

/** Safe failure category for Reference master use cases. */
public enum MasterFailure {
    INVALID_INPUT,
    NOT_FOUND,
    CONFLICT,
    CONCURRENT_MODIFICATION,
    DEPENDENCY_FAILURE
}
