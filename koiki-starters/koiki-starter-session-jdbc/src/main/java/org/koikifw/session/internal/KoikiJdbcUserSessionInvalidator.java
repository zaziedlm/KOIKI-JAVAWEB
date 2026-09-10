package org.koikifw.session.internal;

import java.util.Objects;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.identity.UserSessionInvalidator;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

/** Deletes every persisted Spring Session indexed by one immutable Framework user ID. */
final class KoikiJdbcUserSessionInvalidator implements UserSessionInvalidator {

    private final JdbcIndexedSessionRepository sessionRepository;

    KoikiJdbcUserSessionInvalidator(JdbcIndexedSessionRepository sessionRepository) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository);
    }

    @Override
    public void invalidateAll(FrameworkUserId userId) {
        var sessions = sessionRepository.findByIndexNameAndIndexValue(
                FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
                Objects.requireNonNull(userId).toString());
        for (String sessionId : sessions.keySet()) {
            sessionRepository.deleteById(sessionId);
        }
    }
}
