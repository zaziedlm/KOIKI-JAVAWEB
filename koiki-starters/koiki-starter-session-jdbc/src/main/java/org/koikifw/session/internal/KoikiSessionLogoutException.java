package org.koikifw.session.internal;

/** Safe internal failure raised after local logout cleanup when the Session store fails. */
final class KoikiSessionLogoutException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    KoikiSessionLogoutException() {
        super("KOIKI persistent session logout failed");
    }
}
