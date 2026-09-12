package org.koikifw.buildsupport.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;

class MigrationFailureContractFixtureTest {

    @Test
    void keepsOwnerFailuresDistinctAndRejectsUnsafeInitialization() throws SQLException {
        frameworkFailureStopsBeforeCustomerMigration();
        customerFailureKeepsSuccessfulFrameworkHistoryDistinct();
        customerChecksumMismatchFailsStartup();
        sessionInitializerOverrideFailsWithoutCreatingDefaultTables();
    }

    private void frameworkFailureStopsBeforeCustomerMigration() throws SQLException {
        MigrationFixtureSupport.resetPublicSchema();
        JdbcClient jdbcClient = MigrationFixtureSupport.jdbcClient();
        jdbcClient.sql("CREATE TABLE koiki_user (conflicting_id integer PRIMARY KEY)").update();

        assertThatThrownBy(() -> MigrationFixtureSupport.start(Map.of(
                        "spring.flyway.locations", "classpath:db/c1/customer-marker")))
                .isInstanceOf(RuntimeException.class);

        assertThat(MigrationFixtureSupport.tableExists(jdbcClient, "fixture_customer_marker"))
                .isFalse();
        assertThat(MigrationFixtureSupport.tableExists(jdbcClient, "flyway_schema_history"))
                .isFalse();
        assertThat(MigrationFixtureSupport.sqlVersions(jdbcClient, "koiki_flyway_history"))
                .containsExactly("2026090300");
    }

    private void customerFailureKeepsSuccessfulFrameworkHistoryDistinct() throws SQLException {
        MigrationFixtureSupport.resetPublicSchema();
        JdbcClient jdbcClient = MigrationFixtureSupport.jdbcClient();

        assertThatThrownBy(() -> MigrationFixtureSupport.start(Map.of(
                        "spring.flyway.locations", "classpath:db/c1/customer-failure")))
                .isInstanceOf(RuntimeException.class);

        assertThat(MigrationFixtureSupport.sqlVersions(jdbcClient, "koiki_flyway_history"))
                .containsExactly("2026090300", "2026090301", "2026090701");
        assertThat(MigrationFixtureSupport.count(jdbcClient, """
                        SELECT count(*)
                        FROM koiki_flyway_history
                        WHERE NOT success
                        """))
                .isZero();
        assertThat(MigrationFixtureSupport.tableExists(jdbcClient, "flyway_schema_history"))
                .isTrue();
        assertThat(MigrationFixtureSupport.tableExists(jdbcClient, "fixture_customer_failure"))
                .isFalse();
    }

    private void customerChecksumMismatchFailsStartup() throws SQLException {
        MigrationFixtureSupport.resetPublicSchema();
        JdbcClient jdbcClient = MigrationFixtureSupport.jdbcClient();
        Map<String, Object> customerMarker = Map.of(
                "spring.flyway.locations", "classpath:db/c1/customer-marker");
        try (ConfigurableApplicationContext ignored = MigrationFixtureSupport.start(customerMarker)) {
            assertThat(MigrationFixtureSupport.sqlVersions(jdbcClient, "flyway_schema_history"))
                    .containsExactly("1");
        }
        Integer checksum = jdbcClient.sql("""
                        SELECT checksum
                        FROM flyway_schema_history
                        WHERE version = '1' AND type = 'SQL'
                        """)
                .query(Integer.class)
                .single();
        jdbcClient.sql("""
                        UPDATE flyway_schema_history
                        SET checksum = :checksum
                        WHERE version = '1' AND type = 'SQL'
                        """)
                .param("checksum", checksum + 1)
                .update();

        assertThatThrownBy(() -> MigrationFixtureSupport.start(customerMarker))
                .isInstanceOf(RuntimeException.class);
        assertThat(MigrationFixtureSupport.sqlVersions(jdbcClient, "koiki_flyway_history"))
                .containsExactly("2026090300", "2026090301", "2026090701");
        assertThat(MigrationFixtureSupport.count(
                        jdbcClient, "SELECT count(*) FROM fixture_customer_marker"))
                .isZero();
    }

    private void sessionInitializerOverrideFailsWithoutCreatingDefaultTables()
            throws SQLException {
        MigrationFixtureSupport.resetPublicSchema();
        JdbcClient jdbcClient = MigrationFixtureSupport.jdbcClient();

        assertThatThrownBy(() -> MigrationFixtureSupport.start(Map.of(
                        "spring.session.jdbc.initialize-schema", "always")))
                .isInstanceOf(RuntimeException.class);

        assertThat(MigrationFixtureSupport.applicationTables(jdbcClient))
                .contains("koiki_session", "koiki_session_attributes")
                .doesNotContain("spring_session", "spring_session_attributes");
        assertThat(MigrationFixtureSupport.sqlVersions(jdbcClient, "koiki_flyway_history"))
                .containsExactlyElementsOf(List.of(
                        "2026090300", "2026090301", "2026090701"));
    }
}
