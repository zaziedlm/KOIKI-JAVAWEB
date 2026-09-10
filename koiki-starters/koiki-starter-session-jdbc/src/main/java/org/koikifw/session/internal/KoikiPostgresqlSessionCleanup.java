package org.koikifw.session.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.koikifw.session.SessionCleanup;
import org.koikifw.session.SessionCleanupException;
import org.koikifw.session.SessionCleanupResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

final class KoikiPostgresqlSessionCleanup implements SessionCleanup {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(KoikiPostgresqlSessionCleanup.class);
    private static final int LOCK_NAMESPACE = 1263487305;
    private static final int LOCK_TASK = 1;
    private static final String TRY_LOCK_SQL = "SELECT pg_try_advisory_lock(?, ?)";
    private static final String UNLOCK_SQL = "SELECT pg_advisory_unlock(?, ?)";

    private final DataSource dataSource;
    private final JdbcIndexedSessionRepository sessionRepository;

    KoikiPostgresqlSessionCleanup(
            DataSource dataSource, JdbcIndexedSessionRepository sessionRepository) {
        this.dataSource = dataSource;
        this.sessionRepository = sessionRepository;
    }

    @Override
    public SessionCleanupResult cleanUpExpiredSessions() {
        try (Connection lockConnection = dataSource.getConnection()) {
            if (!executeLockFunction(lockConnection, TRY_LOCK_SQL)) {
                return SessionCleanupResult.CONTENDED;
            }

            boolean cleanupCompleted = false;
            boolean unlockCompleted;
            try {
                sessionRepository.cleanUpExpiredSessions();
                cleanupCompleted = true;
            } catch (RuntimeException exception) {
                // The public failure deliberately does not retain datastore details.
            } finally {
                unlockCompleted = unlock(lockConnection);
            }

            if (!cleanupCompleted || !unlockCompleted) {
                throw failure();
            }
            return SessionCleanupResult.COMPLETED;
        } catch (SQLException exception) {
            throw failure();
        }
    }

    private static boolean unlock(Connection connection) {
        try {
            return executeLockFunction(connection, UNLOCK_SQL);
        } catch (SQLException exception) {
            return false;
        }
    }

    private static boolean executeLockFunction(Connection connection, String sql)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, LOCK_NAMESPACE);
            statement.setInt(2, LOCK_TASK);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getBoolean(1);
            }
        }
    }

    private static SessionCleanupException failure() {
        LOGGER.warn("KOIKI_SESSION_CLEANUP_FAILED");
        return new SessionCleanupException();
    }
}
