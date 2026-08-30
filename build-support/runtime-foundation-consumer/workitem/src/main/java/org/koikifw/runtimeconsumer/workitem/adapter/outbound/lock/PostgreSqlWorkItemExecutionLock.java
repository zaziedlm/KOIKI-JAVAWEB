package org.koikifw.runtimeconsumer.workitem.adapter.outbound.lock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;
import org.koikifw.runtimeconsumer.workitem.application.port.outbound.WorkItemExecutionLock;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Holds a PostgreSQL session advisory lock on one dedicated JDBC connection. */
public final class PostgreSqlWorkItemExecutionLock implements WorkItemExecutionLock {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgreSqlWorkItemExecutionLock.class);
    private static final String TRY_LOCK_SQL = "select pg_try_advisory_lock(?, ?)";
    private static final String UNLOCK_SQL = "select pg_advisory_unlock(?, ?)";

    private final DataSource dataSource;

    public PostgreSqlWorkItemExecutionLock(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<LockHandle> tryAcquire(int namespace, int taskId) {
        @Nullable Connection connection = null;
        try {
            Connection acquiredConnection = dataSource.getConnection();
            connection = acquiredConnection;
            try (PreparedStatement statement = acquiredConnection.prepareStatement(TRY_LOCK_SQL)) {
                statement.setInt(1, namespace);
                statement.setInt(2, taskId);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next() && result.getBoolean(1)) {
                        return Optional.of(new PostgreSqlLockHandle(
                                acquiredConnection, namespace, taskId));
                    }
                }
            }
            connection.close();
            return Optional.empty();
        } catch (SQLException exception) {
            closeAfterFailure(connection);
            throw new IllegalStateException("Unable to acquire maintenance execution lock", exception);
        }
    }

    private static void closeAfterFailure(@Nullable Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException closeFailure) {
            LOGGER.atWarn()
                    .addKeyValue("operation", "releaseWorkItemExecutionLock")
                    .addKeyValue("result", "failed")
                    .addKeyValue("errorCode", "LOCK_CONNECTION_CLOSE_FAILED")
                    .log("maintenance lock connection close failed");
        }
    }

    private static final class PostgreSqlLockHandle implements LockHandle {

        private final Connection connection;
        private final int namespace;
        private final int taskId;
        private boolean closed;

        private PostgreSqlLockHandle(Connection connection, int namespace, int taskId) {
            this.connection = connection;
            this.namespace = namespace;
            this.taskId = taskId;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            try (PreparedStatement statement = connection.prepareStatement(UNLOCK_SQL)) {
                statement.setInt(1, namespace);
                statement.setInt(2, taskId);
                statement.execute();
            } catch (SQLException exception) {
                LOGGER.atWarn()
                        .addKeyValue("operation", "releaseWorkItemExecutionLock")
                        .addKeyValue("result", "failed")
                        .addKeyValue("errorCode", "LOCK_RELEASE_FAILED")
                        .log("maintenance lock release failed; closing the session");
            } finally {
                closeAfterFailure(connection);
            }
        }
    }
}
