package org.koikifw.identity;

/** Authentication mechanism that established a framework principal. */
public enum AuthenticationSource {
    LOCAL,
    OIDC,
    BEARER,
    EDGE
}
