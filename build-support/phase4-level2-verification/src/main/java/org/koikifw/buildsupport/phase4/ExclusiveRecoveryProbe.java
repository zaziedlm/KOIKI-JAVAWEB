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

    /** Conservative fixture entry. The confirmation file is an operator input, not a liveness proof. */
    public void recoverGuarded(
            UUID publicationId, UUID eventId, String expectedStatus, int expectedAttempts,
            Path stopConfirmation, Path statusFile)
            throws SQLException, IOException, InterruptedException {
        String expectedConfirmation = publicationId + "|" + eventId + "|"
                + expectedStatus + "|" + expectedAttempts;
        if (stopConfirmation == null || !Files.isRegularFile(stopConfirmation)
                || !expectedConfirmation.equals(Files.readString(stopConfirmation).trim())) {
            Files.writeString(statusFile, "STOP_UNCONFIRMED");
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            if (!lock(connection, "SELECT pg_try_advisory_lock(?, ?)")) {
                Files.writeString(statusFile, "CONTENDED");
                return;
            }
            try {
                if (jdbc.queryForObject("""
                        SELECT count(*) FROM event_publication
                        WHERE id = ? AND serialized_event LIKE ?
                          AND status = ? AND completion_attempts = ?
                        """, Integer.class, publicationId, "%" + eventId + "%",
                        expectedStatus, expectedAttempts) != 1) {
                    Files.writeString(statusFile, "STALE_SELECTION");
                    return;
                }
                Files.writeString(statusFile, "ACQUIRED");
                incomplete.resubmitIncompletePublications(publication ->
                        publication.getIdentifier().equals(publicationId)
                                && publication.getStatus().name().equals(expectedStatus)
                                && publication.getCompletionAttempts() == expectedAttempts
                                && publication.getEvent() instanceof ProbeApproved event
                                && event.eventId().equals(eventId));
                while (publicationCount(eventId, "COMPLETED") == 0
                        && publicationCount(eventId, "FAILED") == 0) {
                    if (!connection.isValid(1)) {
                        failStop(statusFile);
                    }
                    Thread.sleep(Duration.ofMillis(100));
                }
            } catch (SQLException | RuntimeException exception) {
                failStop(statusFile);
            } finally {
                try {
                    if (!connection.isValid(1)
                            || !lock(connection, "SELECT pg_advisory_unlock(?, ?)")) {
                        failStop(statusFile);
                    }
                } catch (SQLException exception) {
                    failStop(statusFile);
                }
            }
        }
    }

    private static void failStop(Path statusFile) throws IOException {
        try {
            Files.writeString(statusFile, "LOCK_LOST");
        } finally {
            Runtime.getRuntime().halt(70);
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
