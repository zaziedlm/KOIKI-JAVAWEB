package org.koikifw.session.internal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Objects;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.session.web.http.CookieSerializer;

/** Invalidates the persistent Session while always removing local authentication state. */
final class KoikiSessionLogoutHandler implements LogoutHandler {

    private static final Log LOGGER = LogFactory.getLog(KoikiSessionLogoutHandler.class);

    private final CookieSerializer cookieSerializer;
    private final SecurityContextHolderStrategy contextHolderStrategy;

    KoikiSessionLogoutHandler(CookieSerializer cookieSerializer) {
        this.cookieSerializer = Objects.requireNonNull(cookieSerializer);
        this.contextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
    }

    @Override
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            @Nullable Authentication authentication) {
        RuntimeException persistentFailure = null;
        HttpSession session = request.getSession(false);
        if (session != null) {
            try {
                session.invalidate();
            } catch (RuntimeException failure) {
                persistentFailure = failure;
            }
        }

        if (authentication instanceof CredentialsContainer credentialsContainer) {
            credentialsContainer.eraseCredentials();
        }
        contextHolderStrategy.clearContext();
        cookieSerializer.writeCookieValue(new CookieSerializer.CookieValue(request, response, ""));

        if (persistentFailure != null) {
            LOGGER.error("KOIKI persistent session logout failed; local state was cleared");
            throw new KoikiSessionLogoutException();
        }
    }
}
