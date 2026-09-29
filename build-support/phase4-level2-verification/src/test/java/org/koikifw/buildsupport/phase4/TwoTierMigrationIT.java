package org.koikifw.buildsupport.phase4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.DriverManager;
import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Compares two migration owners using the KOIKI-first, separate-history contract. */
class TwoTierMigrationIT {

    @Test
    void frameworkOwnedPublicationUpgradesIndependentlyAndFailedDdlRollsBack() throws SQLException {
        try (var postgres = new PostgreSQLContainer("postgres:17-alpine")) {
            postgres.start();
            Flyway koikiV1 = flyway(postgres, "koiki_flyway_history", "classpath:phase4-v5/koiki");
            Flyway application = flyway(postgres, "flyway_schema_history", "classpath:phase4-v5/application");

            assertEquals(1, koikiV1.migrate().migrationsExecuted);
            assertTrue(tableExists(postgres, "event_publication"));
            assertEquals(1, application.migrate().migrationsExecuted);
            assertTrue(tableExists(postgres, "probe_approval"));
            assertEquals(1, appliedSql(postgres, "koiki_flyway_history"));
            assertEquals(1, appliedSql(postgres, "flyway_schema_history"));
            assertEquals(1, historyRows(postgres, "flyway_schema_history", "BASELINE"));
            assertEquals(0, koikiV1.migrate().migrationsExecuted);
            assertEquals(0, application.migrate().migrationsExecuted);

            Flyway koikiV2 = flyway(postgres, "koiki_flyway_history",
                    "classpath:phase4-v5/koiki", "classpath:phase4-v5/koiki-upgrade");
            assertEquals(1, koikiV2.migrate().migrationsExecuted);
            assertEquals(2, appliedSql(postgres, "koiki_flyway_history"));
            assertEquals(1, appliedSql(postgres, "flyway_schema_history"));
            assertTrue(indexExists(postgres, "event_publication_status_date_idx"));

            Flyway broken = flyway(postgres, "koiki_flyway_history",
                    "classpath:phase4-v5/koiki", "classpath:phase4-v5/koiki-upgrade",
                    "classpath:phase4-v5/koiki-broken");
            assertThrows(FlywayException.class, broken::migrate);
            assertEquals(2, appliedSql(postgres, "koiki_flyway_history"));
            assertEquals(1, appliedSql(postgres, "flyway_schema_history"));
            assertFalse(tableExists(postgres, "probe_failed_upgrade"));
        }
    }

    @Test
    void applicationOwnedPublicationKeepsFrameworkHistorySeparate() throws SQLException {
        try (var postgres = new PostgreSQLContainer("postgres:17-alpine")) {
            postgres.start();
            Flyway koiki = flyway(postgres, "koiki_flyway_history", "classpath:phase4-v5/koiki-marker");
            Flyway application = flyway(postgres, "flyway_schema_history",
                    "classpath:phase4-v5/application-owned");

            assertEquals(1, koiki.migrate().migrationsExecuted);
            assertFalse(tableExists(postgres, "event_publication"));
            assertEquals(1, application.migrate().migrationsExecuted);
            assertTrue(tableExists(postgres, "event_publication"));
            assertTrue(tableExists(postgres, "probe_approval"));
            assertEquals(1, appliedSql(postgres, "koiki_flyway_history"));
            assertEquals(1, appliedSql(postgres, "flyway_schema_history"));
            assertEquals(1, historyRows(postgres, "flyway_schema_history", "BASELINE"));
            assertEquals(0, koiki.migrate().migrationsExecuted);
            assertEquals(0, application.migrate().migrationsExecuted);
        }
    }

    private static Flyway flyway(PostgreSQLContainer postgres, String history, String... locations) {
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations(locations)
                .table(history)
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("0"))
                .load();
    }

    private static int appliedSql(PostgreSQLContainer postgres, String history) throws SQLException {
        return historyRows(postgres, history, "SQL");
    }

    private static int historyRows(PostgreSQLContainer postgres, String history, String type) throws SQLException {
        try (var connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.prepareStatement(
                        "SELECT count(*) FROM " + history + " WHERE type = ? AND success")) {
            statement.setString(1, type);
            try (var rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        }
    }

    private static boolean tableExists(PostgreSQLContainer postgres, String table) throws SQLException {
        try (var connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.prepareStatement("SELECT to_regclass(?)")) {
            statement.setString(1, table);
            try (var rows = statement.executeQuery()) {
                rows.next();
                return rows.getObject(1) != null;
            }
        }
    }

    private static boolean indexExists(PostgreSQLContainer postgres, String index) throws SQLException {
        try (var connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.prepareStatement("SELECT to_regclass(?)")) {
            statement.setString(1, index);
            try (var rows = statement.executeQuery()) {
                rows.next();
                return rows.getObject(1) != null;
            }
        }
    }
}
