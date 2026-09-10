package org.koikifw.identity;

import java.util.Set;

/** Stable application-facing identity established by an authentication adapter. */
public interface FrameworkPrincipal {

    FrameworkUserId userId();

    AuthenticationSource authenticationSource();

    Set<String> permissions();
}
