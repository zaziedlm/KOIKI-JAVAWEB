package org.koikifw.reference.identity.adapter.inbound.web;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Web input for one optimistic Role membership change. */
record IdentityRoleChangeForm(@Nullable String roleCode, @Nullable Long expectedVersion) {

    private static final String INVALID_ROLE_CHANGE = "Invalid role change request.";

    String requiredRoleCode() {
        if (roleCode == null || roleCode.isBlank()) {
            throw invalidRequest();
        }
        return roleCode;
    }

    long requiredExpectedVersion() {
        if (expectedVersion == null) {
            throw invalidRequest();
        }
        return expectedVersion;
    }

    private static ResponseStatusException invalidRequest() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_ROLE_CHANGE);
    }
}
