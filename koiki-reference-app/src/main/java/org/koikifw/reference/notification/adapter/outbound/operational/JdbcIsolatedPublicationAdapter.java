package org.koikifw.reference.notification.adapter.outbound.operational;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import javax.sql.DataSource;
import org.koikifw.reference.expense.domain.event.ExpenseApprovedEvent;
import org.koikifw.reference.notification.application.port.outbound.IsolatedPublicationPort;
import org.koikifw.reference.notification.application.query.IsolatedRecoverySelection;
import tools.jackson.databind.ObjectMapper;

/** R3-A-only read adapter and session-scoped lock; publication state changes belong to Modulith. */
public final class JdbcIsolatedPublicationAdapter implements IsolatedPublicationPort {
    private static final long LOCK_KEY = 0x4b4b524546523341L;
    private static final String EVENT_TYPE = ExpenseApprovedEvent.class.getName();
    private static final String SELECT = """
            SELECT id, listener_id, event_type, serialized_event, status,
                   completion_attempts, completion_date
            FROM kkref_notification_async.event_publication WHERE id = ?
            """;
    private final DataSource dataSource;
    private final ObjectMapper mapper;

    public JdbcIsolatedPublicationAdapter(DataSource dataSource, ObjectMapper mapper) {
        this.dataSource = Objects.requireNonNull(dataSource);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Lease acquire(IsolatedRecoverySelection selection) {
        Connection connection = null;
        boolean locked = false;
        try {
            connection = dataSource.getConnection();
            try (var statement = connection.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
                statement.setLong(1, LOCK_KEY);
                statement.setQueryTimeout(5);
                try (var result = statement.executeQuery()) {
                    result.next();
                    locked = result.getBoolean(1);
                }
            }
            if (!locked) throw new IllegalStateException("Isolated recovery lock unavailable");
            var lease = new DbLease(connection, selection, backendPid(connection));
            lease.assertUnchanged();
            return lease;
        } catch (SQLException | RuntimeException failure) {
            if (connection != null) {
                if (locked) unlockQuietly(connection);
                try { connection.close(); } catch (SQLException closeFailure) { failure.addSuppressed(closeFailure); }
            }
            throw new IllegalStateException("Isolated recovery admission rejected", failure);
        }
    }

    private static int backendPid(Connection connection) throws SQLException {
        try (var statement = connection.createStatement(); var result = statement.executeQuery("SELECT pg_backend_pid()")) {
            result.next();
            return result.getInt(1);
        }
    }

    private static void unlockQuietly(Connection connection) {
        try (var statement = connection.prepareStatement("SELECT pg_advisory_unlock(?)")) {
            statement.setLong(1, LOCK_KEY);
            statement.executeQuery().close();
        } catch (SQLException ignored) {
            // Closing a broken connection releases its session lock at PostgreSQL.
        }
    }

    private final class DbLease implements Lease {
        private final Connection connection;
        private final IsolatedRecoverySelection selection;
        private final int pid;
        private boolean closed;

        private DbLease(Connection connection, IsolatedRecoverySelection selection, int pid) {
            this.connection = connection;
            this.selection = selection;
            this.pid = pid;
        }

        @Override
        public void assertUnchanged() {
            if (closed) throw new IllegalStateException("Isolated recovery lease closed");
            try {
                if (!connection.isValid(2) || backendPid(connection) != pid) {
                    throw new IllegalStateException("Isolated recovery lock connection changed");
                }
                assertAdmission();
                Row row = read();
                if (row == null || !selection.publicationId().equals(row.id)
                        || !selection.listenerId().equals(row.listenerId)
                        || !EVENT_TYPE.equals(row.eventType)
                        || !selection.status().equals(row.status)
                        || selection.completionAttempts() != row.attempts
                        || row.completed) {
                    throw new IllegalStateException("Isolated recovery target snapshot changed");
                }
                ExpenseApprovedEvent event = decode(row.serializedEvent);
                if (!selection.eventId().equals(event.eventId())
                        || !selection.notificationKey().equals(event.notificationKey())
                        || !selection.payloadSha256().equals(event.payloadSha256())) {
                    throw new IllegalStateException("Isolated recovery event or payload changed");
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Isolated recovery target unavailable", exception);
            }
        }

        private void assertAdmission() throws SQLException {
            try (var statement = connection.prepareStatement("""
                    SELECT environment_id, publication_id, event_id
                    FROM kkref_r3_recovery.authorized_target
                    """)) {
                statement.setQueryTimeout(5);
                try (var result = statement.executeQuery()) {
                    if (!result.next() || !selection.environmentId().equals(result.getString(1))
                            || !selection.publicationId().equals(result.getObject(2, UUID.class))
                            || !selection.eventId().equals(result.getObject(3, UUID.class))
                            || result.next()) {
                        throw new IllegalStateException("Isolated recovery target not authorized in fresh DB");
                    }
                }
            }
        }

        @Override
        public ExpenseApprovedEvent event() {
            assertUnchanged();
            try {
                Row row = read();
                if (row == null) throw new IllegalStateException("Isolated recovery event absent");
                return decode(row.serializedEvent);
            } catch (SQLException exception) {
                throw new IllegalStateException("Isolated recovery event unavailable", exception);
            }
        }

        @Override
        public void awaitCompleted(Duration timeout) {
            Instant deadline = Instant.now().plus(timeout);
            while (Instant.now().isBefore(deadline)) {
                try {
                    if (closed || !connection.isValid(2) || backendPid(connection) != pid) {
                        throw new IllegalStateException("Isolated recovery lock lost");
                    }
                    Row row = read();
                    if (row == null || !selection.publicationId().equals(row.id)
                            || !selection.listenerId().equals(row.listenerId)
                            || !EVENT_TYPE.equals(row.eventType)
                            || !selection.eventId().equals(decode(row.serializedEvent).eventId())) {
                        throw new IllegalStateException("Isolated recovery target changed");
                    }
                    if (row.completed && "COMPLETED".equals(row.status)
                            && row.attempts == selection.completionAttempts() + 1) return;
                    if ("FAILED".equals(row.status) || row.completed) {
                        throw new IllegalStateException("Isolated recovery did not complete the selected publication");
                    }
                    Thread.sleep(100);
                } catch (SQLException exception) {
                    throw new IllegalStateException("Isolated recovery completion unavailable", exception);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Isolated recovery interrupted", exception);
                }
            }
            throw new IllegalStateException("Isolated recovery completion deadline");
        }

        private Row read() throws SQLException {
            try (PreparedStatement statement = connection.prepareStatement(SELECT)) {
                statement.setObject(1, selection.publicationId());
                statement.setQueryTimeout(5);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) return null;
                    UUID id = result.getObject("id", UUID.class);
                    String listenerId = result.getString("listener_id");
                    String eventType = result.getString("event_type");
                    String serializedEvent = result.getString("serialized_event");
                    String status = result.getString("status");
                    int attempts = result.getInt("completion_attempts");
                    if (result.wasNull()) throw new IllegalStateException("Publication attempt count absent");
                    boolean completed = result.getTimestamp("completion_date") != null;
                    if (result.next()) throw new IllegalStateException("Duplicate publication ID");
                    return new Row(id, listenerId, eventType, serializedEvent, status, attempts, completed);
                }
            }
        }

        private ExpenseApprovedEvent decode(String serialized) {
            try {
                return mapper.readValue(serialized, ExpenseApprovedEvent.class);
            } catch (RuntimeException invalid) {
                throw new IllegalStateException("Isolated recovery event unreadable", invalid);
            }
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            boolean unlocked = false;
            SQLException releaseFailure = null;
            try (var statement = connection.prepareStatement("SELECT pg_advisory_unlock(?)")) {
                statement.setLong(1, LOCK_KEY);
                try (var result = statement.executeQuery()) {
                    result.next();
                    unlocked = result.getBoolean(1);
                }
            } catch (SQLException failure) {
                releaseFailure = failure;
            }
            try { connection.close(); } catch (SQLException failure) {
                if (releaseFailure != null) failure.addSuppressed(releaseFailure);
                throw new IllegalStateException("Isolated recovery connection close failed", failure);
            }
            if (releaseFailure != null) throw new IllegalStateException("Isolated recovery lock release unknown", releaseFailure);
            if (!unlocked) throw new IllegalStateException("Isolated recovery lock was lost");
        }
    }

    private record Row(UUID id, String listenerId, String eventType, String serializedEvent,
            String status, int attempts, boolean completed) { }
}
