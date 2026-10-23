package org.koikifw.reference.identity.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.web.filter.OncePerRequestFilter;

/** Installed only in Security chains, after CSRF and immediately before logout. */
final class ReferenceSessionLogoutBoundary extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(ReferenceSessionLogoutBoundary.class);
    private final String path;
    private final CookieCsrfTokenRepository csrf;
    private final CookieSerializer sessions;
    private final ReferenceSessionApiProblemWriter problems;

    ReferenceSessionLogoutBoundary(String path, CookieCsrfTokenRepository csrf,
            CookieSerializer sessions, ReferenceSessionApiProblemWriter problems) {
        this.path = path;
        this.csrf = csrf;
        this.sessions = sessions;
        this.problems = problems;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod())
                || !(request.getContextPath() + path).equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        @Nullable RuntimeException failure = null;
        try {
            chain.doFilter(request, response);
        } catch (RuntimeException exception) {
            failure = exception;
        } finally {
            SecurityContextHolder.clearContext();
            if (!response.isCommitted()) {
                try {
                    try {
                        csrf.saveToken(null, request, response);
                    } finally {
                        sessions.writeCookieValue(new CookieSerializer.CookieValue(request, response, ""));
                    }
                } catch (RuntimeException exception) {
                    failure = exception;
                }
            }
        }
        if (failure != null) {
            // Do not log the exception, credentials, session identifier or provider details.
            LOG.error("Reference logout failed; persistent session invalidation is unconfirmed");
            if (response.isCommitted()) {
                throw new ServletException("Reference logout response was already committed");
            }
            response.resetBuffer();
            if (path.startsWith("/spa-api/")) {
                problems.write(request, response, HttpStatus.SERVICE_UNAVAILABLE,
                        "Session invalidation could not be confirmed.", "KOIKI-REF-SESSION-003");
            } else {
                response.setStatus(503);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("Session invalidation could not be confirmed.");
            }
        }
    }
}
