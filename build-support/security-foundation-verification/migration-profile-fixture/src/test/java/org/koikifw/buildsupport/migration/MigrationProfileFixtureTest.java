package org.koikifw.buildsupport.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;

class MigrationProfileFixtureTest {

    private static final List<String> AUDIT_TABLES = List.of("koiki_audit_event");
    private static final List<String> IDENTITY_TABLES = List.of(
            "koiki_audit_event",
            "koiki_external_identity_link",
            "koiki_login_attempt",
            "koiki_password_credential",
            "koiki_permission",
            "koiki_role",
            "koiki_role_permission",
            "koiki_user",
            "koiki_user_role");
    private static final List<String> SESSION_TABLES = List.of(
            "koiki_audit_event",
            "koiki_external_identity_link",
            "koiki_login_attempt",
            "koiki_password_credential",
            "koiki_permission",
            "koiki_role",
            "koiki_role_permission",
            "koiki_session",
            "koiki_session_attributes",
            "koiki_user",
            "koiki_user_role");

    @Test
    void verifiesCleanProfileOrSupportedUpgrade() {
        String profile = System.getProperty("koiki.fixture.profile", "");
        if (profile.equals("upgrade")) {
            verifyPhase1bUpgrade();
            return;
        }
        verifyCleanProfile(profile);
    }

    private void verifyCleanProfile(String profile) {
        ProfileExpectation expectation = ProfileExpectation.forName(profile);
        List<String> firstFrameworkHistory;
        List<String> firstCustomerHistory;
        try (ConfigurableApplicationContext context = MigrationFixtureSupport.start()) {
            JdbcClient jdbcClient = MigrationFixtureSupport.jdbcClient(
                    context.getBean(DataSource.class));
            assertProfile(context, jdbcClient, expectation);
            firstFrameworkHistory = MigrationFixtureSupport.history(
                    jdbcClient, "koiki_flyway_history");
            firstCustomerHistory = MigrationFixtureSupport.history(
                    jdbcClient, "flyway_schema_history");
        }

        try (ConfigurableApplicationContext context = MigrationFixtureSupport.start()) {
            JdbcClient jdbcClient = MigrationFixtureSupport.jdbcClient(
                    context.getBean(DataSource.class));
            assertProfile(context, jdbcClient, expectation);
            assertThat(MigrationFixtureSupport.history(jdbcClient, "koiki_flyway_history"))
                    .containsExactlyElementsOf(firstFrameworkHistory);
            assertThat(MigrationFixtureSupport.history(jdbcClient, "flyway_schema_history"))
                    .containsExactlyElementsOf(firstCustomerHistory);
        }
    }

    private void verifyPhase1bUpgrade() {
        Flyway phase1bFramework = Flyway.configure()
                .dataSource(
                        MigrationFixtureSupport.jdbcUrl(),
                        MigrationFixtureSupport.username(),
                        MigrationFixtureSupport.password())
                .locations("classpath:db/c1/phase1b-empty")
                .table("koiki_flyway_history")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
        assertThat(phase1bFramework.migrate().migrationsExecuted).isZero();

        Flyway phase1bCustomer = Flyway.configure()
                .dataSource(
                        MigrationFixtureSupport.jdbcUrl(),
                        MigrationFixtureSupport.username(),
                        MigrationFixtureSupport.password())
                .locations("classpath:db/migration/customer")
                .table("flyway_schema_history")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
        assertThat(phase1bCustomer.migrate().migrationsExecuted).isEqualTo(3);

        JdbcClient baselineJdbc = MigrationFixtureSupport.jdbcClient();
        UUID seedId = UUID.fromString("00000000-0000-4000-8000-00000000c103");
        baselineJdbc.sql("INSERT INTO kkbiz_work_item(id, label) VALUES (:id, 'phase1b-seed')")
                .param("id", seedId)
                .update();
        List<String> baselineCustomerHistory = MigrationFixtureSupport.history(
                baselineJdbc, "flyway_schema_history");
        assertThat(MigrationFixtureSupport.sqlVersions(
                        baselineJdbc, "koiki_flyway_history"))
                .isEmpty();
        assertThat(MigrationFixtureSupport.sqlVersions(
                        baselineJdbc, "flyway_schema_history"))
                .containsExactly("1", "3", "4");
        assertThat(baselineCustomerHistory)
                .anySatisfy(row -> assertThat(row).contains("|0|<< Flyway Baseline >>|BASELINE|"));

        List<String> firstFrameworkHistory;
        try (ConfigurableApplicationContext context = MigrationFixtureSupport.start()) {
            JdbcClient jdbcClient = MigrationFixtureSupport.jdbcClient(
                    context.getBean(DataSource.class));
            assertProfile(context, jdbcClient, ProfileExpectation.SESSION);
            assertPhase1bState(jdbcClient, seedId, baselineCustomerHistory);
            firstFrameworkHistory = MigrationFixtureSupport.history(
                    jdbcClient, "koiki_flyway_history");
        }

        try (ConfigurableApplicationContext context = MigrationFixtureSupport.start()) {
            JdbcClient jdbcClient = MigrationFixtureSupport.jdbcClient(
                    context.getBean(DataSource.class));
            assertProfile(context, jdbcClient, ProfileExpectation.SESSION);
            assertPhase1bState(jdbcClient, seedId, baselineCustomerHistory);
            assertThat(MigrationFixtureSupport.history(jdbcClient, "koiki_flyway_history"))
                    .containsExactlyElementsOf(firstFrameworkHistory);
        }
    }

    private void assertPhase1bState(
            JdbcClient jdbcClient, UUID seedId, List<String> baselineCustomerHistory) {
        assertThat(MigrationFixtureSupport.history(jdbcClient, "flyway_schema_history"))
                .containsExactlyElementsOf(baselineCustomerHistory);
        assertThat(jdbcClient.sql("SELECT label FROM kkbiz_work_item WHERE id = :id")
                        .param("id", seedId)
                        .query(String.class)
                        .single())
                .isEqualTo("phase1b-seed");
        assertThat(MigrationFixtureSupport.tableExists(jdbcClient, "kkbiz_work_item")).isTrue();
        assertThat(MigrationFixtureSupport.tableExists(jdbcClient, "kkbiz_work_review")).isTrue();
        assertThat(MigrationFixtureSupport.tableExists(
                        jdbcClient, "kkbiz_work_item_maintenance"))
                .isTrue();
    }

    private void assertProfile(
            ConfigurableApplicationContext context,
            JdbcClient jdbcClient,
            ProfileExpectation expectation) {
        assertThat(MigrationFixtureSupport.applicationTables(jdbcClient))
                .containsExactlyElementsOf(expectation.tables());
        assertThat(MigrationFixtureSupport.sqlVersions(jdbcClient, "koiki_flyway_history"))
                .containsExactlyElementsOf(expectation.versions());
        assertThat(MigrationFixtureSupport.count(jdbcClient, """
                        SELECT count(*)
                        FROM koiki_flyway_history
                        WHERE NOT success
                        """))
                .isZero();
        assertThat(MigrationFixtureSupport.count(jdbcClient, """
                        SELECT count(*)
                        FROM flyway_schema_history
                        WHERE NOT success
                        """))
                .isZero();
        assertThat(MigrationFixtureSupport.count(jdbcClient, """
                        SELECT count(*)
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = ANY (ARRAY[%s])
                        """.formatted(quotedTables(expectation.tables()))))
                .isEqualTo(expectation.columnCount());
        assertThat(MigrationFixtureSupport.count(jdbcClient, """
                        SELECT count(*)
                        FROM pg_constraint constraint_record
                        JOIN pg_class table_record ON table_record.oid = constraint_record.conrelid
                        JOIN pg_namespace namespace_record ON namespace_record.oid = table_record.relnamespace
                        WHERE namespace_record.nspname = 'public'
                          AND table_record.relname = ANY (ARRAY[%s])
                        """.formatted(quotedTables(expectation.tables()))))
                .isEqualTo(expectation.constraintCount());
        assertThat(MigrationFixtureSupport.count(jdbcClient, """
                        SELECT count(*)
                        FROM pg_indexes
                        WHERE schemaname = 'public'
                          AND tablename = ANY (ARRAY[%s])
                        """.formatted(quotedTables(expectation.tables()))))
                .isEqualTo(expectation.indexCount());
        assertAuditColumns(jdbcClient);

        Environment environment = context.getEnvironment();
        if (expectation.sessionEnabled()) {
            assertThat(environment.getProperty("spring.session.jdbc.initialize-schema"))
                    .isEqualTo("never");
            assertThat(environment.getProperty("spring.session.jdbc.table-name"))
                    .isEqualTo("koiki_session");
            assertThat(environment.getProperty("spring.session.jdbc.cleanup-cron"))
                    .isEqualTo("-");
            assertThat(MigrationFixtureSupport.applicationTables(jdbcClient))
                    .doesNotContain("spring_session", "spring_session_attributes");
        }
    }

    private void assertAuditColumns(JdbcClient jdbcClient) {
        assertThat(jdbcClient.sql("""
                        SELECT attribute_record.attname || '|' ||
                               pg_catalog.format_type(
                                   attribute_record.atttypid, attribute_record.atttypmod) || '|' ||
                               attribute_record.attnotnull
                        FROM pg_attribute attribute_record
                        JOIN pg_class table_record ON table_record.oid = attribute_record.attrelid
                        JOIN pg_namespace namespace_record ON namespace_record.oid = table_record.relnamespace
                        WHERE namespace_record.nspname = 'public'
                          AND table_record.relname = 'koiki_audit_event'
                          AND attribute_record.attnum > 0
                          AND NOT attribute_record.attisdropped
                        ORDER BY attribute_record.attnum
                        """)
                .query(String.class)
                .list()).containsExactly(
                        "event_id|uuid|true",
                        "audit_type|character varying(16)|true",
                        "event_type|character varying(128)|true",
                        "actor_type|character varying(16)|true",
                        "actor_id|character varying(255)|false",
                        "subject_id|character varying(255)|false",
                        "resource_type|character varying(128)|false",
                        "resource_id|character varying(255)|false",
                        "action|character varying(128)|true",
                        "result|character varying(16)|true",
                        "reason_code|character varying(128)|false",
                        "occurred_at|timestamp(6) with time zone|true",
                        "request_id|character varying(128)|false",
                        "trace_id|character varying(128)|false");
    }

    private String quotedTables(List<String> tables) {
        return tables.stream()
                .map(table -> "'" + table + "'")
                .reduce((left, right) -> left + "," + right)
                .orElseThrow();
    }

    private record ProfileExpectation(
            List<String> tables,
            List<String> versions,
            int columnCount,
            int constraintCount,
            int indexCount,
            boolean sessionEnabled) {

        private static final ProfileExpectation AUDIT = new ProfileExpectation(
                AUDIT_TABLES, List.of("2026090300"), 14, 1, 1, false);
        private static final ProfileExpectation IDENTITY = new ProfileExpectation(
                IDENTITY_TABLES,
                List.of("2026090300", "2026090301"),
                59,
                36,
                16,
                false);
        private static final ProfileExpectation SESSION = new ProfileExpectation(
                SESSION_TABLES,
                List.of("2026090300", "2026090301", "2026090701"),
                69,
                39,
                21,
                true);

        private static ProfileExpectation forName(String profile) {
            return switch (profile) {
                case "audit" -> AUDIT;
                case "identity" -> IDENTITY;
                case "session", "reference" -> SESSION;
                default -> throw new IllegalArgumentException(
                        "Unsupported P2-C1 clean profile: " + profile);
            };
        }
    }
}
