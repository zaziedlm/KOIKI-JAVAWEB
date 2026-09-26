package org.koikifw.buildsupport.phase4;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.stereotype.Component;

/** Tooling-only candidate: one dedicated recovery process at a time, using the CP8 lock pattern. */
@Component
public class ExclusiveRecoveryProbe {

    static final int LOCK_NAMESPACE = 0x4b4f494b;
    static final int LOCK_TASK = 41;

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final IncompleteEventPublications incomplete;

    public ExclusiveRecoveryProbe(
            DataSource dataSource, JdbcTemplate jdbc, IncompleteEventPublications incomplete) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
        this.incomplete = incomplete;
    }

    public void recover(UUID eventId, Path statusFile) throws SQLException, IOException, InterruptedException {
        try (Connection connection = dataSource.getConnection()) {
            if (!lock(connection, "SELECT pg_try_advisory_lock(?, ?)")) {
                Files.writeString(statusFile, "CONTENDED");
                return;
            }
            try {
                Files.writeString(statusFile, "ACQUIRED");
                incomplete.resubmitIncompletePublications(publication ->
                        publication.getEvent() instanceof ProbeApproved event
                                && event.eventId().equals(eventId));
                while (publicationCount(eventId, "COMPLETED") == 0
                        && publicationCount(eventId, "FAILED") == 0) {
                    Thread.sleep(Duration.ofMillis(100));
                }
            } finally {
                if (!lock(connection, "SELECT pg_advisory_unlock(?, ?)")) {
                    throw new IllegalStateException("PL2 recovery lock was not held on release");
                }
            }
        }
    }

    private int publicationCount(UUID eventId, String status) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM event_publication
                WHERE serialized_event LIKE ? AND status = ?
                """, Integer.class, "%" + eventId + "%", status);
    }

    private static boolean lock(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, LOCK_NAMESPACE);
            statement.setInt(2, LOCK_TASK);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getBoolean(1);
            }
        }
    }
}
