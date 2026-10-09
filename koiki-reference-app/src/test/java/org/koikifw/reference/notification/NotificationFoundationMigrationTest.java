package org.koikifw.reference.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.koikifw.reference.ReferenceApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Initial M01-M05 checks with real Reference startup and isolated PostgreSQL. */
@EnabledIfSystemProperty(named = "koiki.reference.verification.resource-limits.enabled", matches = "true")
class NotificationFoundationMigrationTest {
    static final String NORMAL = "classpath:db/migration/kkref";
    static final String EXTRA = "classpath:db/migration/kkref-notification";
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine")
            .withImagePullPolicy(image -> false)
            .withStartupTimeout(Duration.ofSeconds(60))
            .withCommand("postgres", "-c", "max_connections=16", "-c", "lock_timeout=10s",
                    "-c", "statement_timeout=10s", "-c", "transaction_timeout=10s")
            .withCreateContainerCmdModifier(command -> Objects.requireNonNull(command.getHostConfig())
                    .withMemory(1073741824L).withNanoCPUs(1000000000L));

    @BeforeAll
    static void startDatabase() throws Exception {
        assertThat(System.getProperty("koiki.reference.verification.resource-limits.enabled")).isEqualTo("true");
        assertThat(Runtime.getRuntime().maxMemory()).isEqualTo(805306368L);
        POSTGRES.start();
        var host = POSTGRES.getContainerInfo().getHostConfig();
        assertThat(host.getMemory()).isEqualTo(1073741824L);
        assertThat(host.getNanoCPUs()).isEqualTo(1000000000L);
        try (var connection = connection(POSTGRES.getJdbcUrl())) {
            assertThat(rows(connection, "SHOW max_connections")).containsExactly("16");
            assertThat(rows(connection, "SHOW lock_timeout")).containsExactly("10s");
            assertThat(rows(connection, "SHOW statement_timeout")).containsExactly("10s");
            assertThat(rows(connection, "SHOW transaction_timeout")).containsExactly("10s");
        }
        System.out.println("S1_RESOURCE dbId=" + POSTGRES.getContainerId()
                + " memory=1073741824 nanoCpus=1000000000 maxConnections=16 heap=805306368");
    }

    @AfterAll
    static void stopDatabase() {
        POSTGRES.stop();
    }

    static Connection connection(String url) throws Exception {
        return DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    static String newDatabase() throws Exception {
        String name = "s1_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = connection(POSTGRES.getJdbcUrl()); var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + name);
        }
        return POSTGRES.getJdbcUrl().replace("/test", "/" + name);
    }

    static ConfigurableApplicationContext start(String url, String enabled, boolean includeV4) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("server.port", "0");
        properties.put("spring.datasource.url", url);
        properties.put("spring.datasource.username", POSTGRES.getUsername());
        properties.put("spring.datasource.password", POSTGRES.getPassword());
        properties.put("spring.datasource.hikari.maximum-pool-size", "4");
        properties.put("spring.datasource.hikari.minimum-idle", "1");
        properties.put("spring.session.jdbc.initialize-schema", "never");
        properties.put("koiki.identity.local-authentication.enabled", "false");
        properties.put("KOIKI_REFERENCE_SOURCE_HMAC_KEY_ID", "s1-migration-test");
        properties.put("KOIKI_REFERENCE_SOURCE_HMAC_KEY", Base64.getEncoder().encodeToString(new SecureRandom().generateSeed(32)));
        properties.put("spring.main.banner-mode", "off");
        if (!enabled.equals("absent")) properties.put("koiki.reference.notification.foundation.enabled", enabled);
        var application = new SpringApplicationBuilder(ReferenceApplication.class).properties(properties);
        // Default properties cannot override the existing application.properties migration location.
        var context = includeV4 ? application.run("--spring.flyway.locations=" + NORMAL + "," + EXTRA) : application.run();
        var pool = context.getBean(HikariDataSource.class);
        assertThat(pool.getMaximumPoolSize()).isEqualTo(4);
        assertThat(pool.getMinimumIdle()).isEqualTo(1);
        return context;
    }

    static void assertDisabled(ConfigurableApplicationContext context) {
        var entities = context.getBean(EntityManagerFactory.class).getMetamodel().getEntities().stream()
                .map(type -> type.getJavaType().getSimpleName()).collect(Collectors.toSet());
        assertThat(entities).contains("DepartmentEntity", "ExpenseRequest", "IdentityUserEntity")
                .doesNotContain("RecoveryPermit", "RecoveryConsumption");
        assertThat(context.getBeanFactory().getBeanNamesForType(
                org.koikifw.reference.notification.configuration.NotificationFoundationConfiguration.class)).isEmpty();
        assertThat(context.getBeanDefinitionNames()).noneMatch(name -> {
            String className = context.getBeanFactory().getBeanDefinition(name).getBeanClassName();
            return className != null && className.startsWith("org.koikifw.reference.notification.");
        });
    }

    static List<String> rows(Connection connection, String sql) throws Exception {
        List<String> rows = new ArrayList<>();
        try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
            int columns = result.getMetaData().getColumnCount();
            while (result.next()) {
                List<String> fields = new ArrayList<>();
                for (int column = 1; column <= columns; column++) fields.add(String.valueOf(result.getObject(column)));
                rows.add(String.join("|", fields));
            }
        }
        return rows;
    }

    static Flyway managementFlyway(String url, String extra) {
        return Flyway.configure().dataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations(NORMAL, extra).table("kkref_flyway_history").baselineOnMigrate(true)
                .baselineVersion("0").load();
    }

    @Test
    void appliesFreshReferenceSchema() throws Exception {
        String url = newDatabase();
        try (var context = start(url, "true", true); var connection = connection(url)) {
            var names = context.getBean(EntityManagerFactory.class).getMetamodel().getEntities().stream()
                    .map(type -> type.getJavaType().getSimpleName()).collect(Collectors.toSet());
            assertThat(names).contains("RecoveryPermit", "RecoveryConsumption", "DepartmentEntity", "ExpenseRequest", "IdentityUserEntity");
            assertThat(rows(connection, "SELECT version FROM kkref_flyway_history WHERE version <> '0' ORDER BY installed_rank"))
                    .containsExactly("1", "2", "3", "4");
            assertThat(rows(connection, "SELECT count(*) FROM information_schema.tables WHERE table_name IN ('kkref_notification_recovery_permit','kkref_notification_recovery_consumption')"))
                    .containsExactly("2");
            assertThat(rows(connection, "SELECT count(*) FROM koiki_flyway_history WHERE success")).isNotEmpty();
            assertThat(rows(connection, "SELECT count(*) FROM pg_indexes WHERE indexname='uk_kkref_notification_permit_unclosed_target' AND indexdef LIKE '%WHERE (closed_at IS NULL)%'"))
                    .containsExactly("1");
            assertThat(rows(connection, "SELECT count(*) FROM information_schema.table_constraints WHERE table_name='kkref_notification_recovery_consumption' AND constraint_type='FOREIGN KEY'"))
                    .containsExactly("1");
        }
    }

    @Test
    void upgradesWithoutChangingExistingRows() throws Exception {
        String url = newDatabase();
        try (var context = start(url, "absent", false)) { assertDisabled(context); }
        try (var connection = connection(url); var statement = connection.createStatement()) {
            statement.execute("INSERT INTO kkref_department VALUES ('00000000-0000-0000-0000-000000000001','S1_KEEP','Keep',true,0,'2026-10-07T00:00:00Z','2026-10-07T00:00:00Z')");
            var before = rows(connection, "SELECT * FROM kkref_department ORDER BY department_id");
            var history = rows(connection, "SELECT * FROM kkref_flyway_history ORDER BY installed_rank");
            managementFlyway(url, EXTRA).migrate();
            assertThat(rows(connection, "SELECT * FROM kkref_department ORDER BY department_id")).isEqualTo(before);
            assertThat(rows(connection, "SELECT * FROM kkref_flyway_history WHERE version <> '4' ORDER BY installed_rank")).isEqualTo(history);
        }
    }

    @Test
    void preservesExistingStateAfterMigrationFailure() throws Exception {
        String url = newDatabase();
        try (var context = start(url, "absent", false)) { assertDisabled(context); }
        Path location = Files.createTempDirectory("s1-failed-migration-");
        Path script = location.resolve("V4__failed_notification_fixture.sql");
        try (var connection = connection(url)) {
            try (var statement = connection.createStatement()) {
                statement.execute("INSERT INTO kkref_department VALUES ('00000000-0000-0000-0000-000000000002','S1_FAIL_KEEP','Keep',true,0,'2026-10-07T00:00:00Z','2026-10-07T00:00:00Z')");
            }
            var existingRows = rows(connection, "SELECT * FROM kkref_department ORDER BY department_id");
            var history = rows(connection, "SELECT * FROM kkref_flyway_history ORDER BY installed_rank");
            try (var resource = Objects.requireNonNull(NotificationFoundationMigrationTest.class.getResourceAsStream(
                    "/db/migration/kkref-notification/V4__create_notification_recovery_records.sql"))) {
                Files.writeString(script, new String(resource.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                        + "\nSELECT * FROM s1_intentionally_missing_table;\n");
            }
            assertThatThrownBy(() -> managementFlyway(url, "filesystem:" + location).migrate()).isInstanceOf(RuntimeException.class);
            assertThat(rows(connection, "SELECT * FROM kkref_flyway_history ORDER BY installed_rank")).isEqualTo(history);
            assertThat(rows(connection, "SELECT * FROM kkref_department ORDER BY department_id")).isEqualTo(existingRows);
            assertThat(rows(connection, "SELECT count(*) FROM information_schema.tables WHERE table_name LIKE 'kkref_notification_%'"))
                    .containsExactly("0");
        } finally {
            Files.deleteIfExists(script);
            Files.deleteIfExists(location);
        }
    }

    @Test
    void startsDisabledWithOriginalSchema() throws Exception {
        String url = newDatabase();
        try (var context = start(url, "absent", false); var connection = connection(url)) {
            assertDisabled(context);
            assertThat(context.getBean(Flyway.class).validateWithResult().validationSuccessful).isTrue();
            assertThat(rows(connection, "SELECT count(*) FROM information_schema.tables WHERE table_name LIKE 'kkref_notification_%'"))
                    .containsExactly("0");
        }
    }

    @Test
    void startsDisabledWithUpgradedSchema() throws Exception {
        String url = newDatabase();
        try (var context = start(url, "absent", false)) { assertDisabled(context); }
        managementFlyway(url, EXTRA).migrate();
        List<String> permitBefore;
        List<String> consumptionBefore;
        try (var connection = connection(url); var statement = connection.createStatement()) {
            statement.execute("INSERT INTO kkref_notification_recovery_permit (permit_id,environment_id,publication_id,event_id,listener_id,expected_attempt,actor_id,reason_code,issued_at,expires_at) VALUES ('00000000-0000-0000-0000-000000000003','test','00000000-0000-0000-0000-000000000004','00000000-0000-0000-0000-000000000005','test-listener',0,'00000000-0000-0000-0000-000000000006','TEST_ONLY','2026-10-07T00:00:00Z','2026-10-07T00:00:01Z')");
            statement.execute("INSERT INTO kkref_notification_recovery_consumption VALUES ('00000000-0000-0000-0000-000000000003','00000000-0000-0000-0000-000000000007','test-generation','2026-10-07T00:00:00Z')");
            permitBefore = rows(connection, "SELECT * FROM kkref_notification_recovery_permit");
            consumptionBefore = rows(connection, "SELECT * FROM kkref_notification_recovery_consumption");
        }
        try (var context = start(url, "false", false); var connection = connection(url)) {
            assertDisabled(context);
            var flyway = context.getBean(Flyway.class);
            assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
            assertThat(flyway.getConfiguration().getLocations()).extracting(Object::toString).containsExactly(NORMAL);
            assertThat(rows(connection, "SELECT count(*) FROM kkref_flyway_history WHERE version='4' AND success")).containsExactly("1");
            assertThat(rows(connection, "SELECT count(*) FROM information_schema.tables WHERE table_name LIKE 'kkref_notification_%'"))
                    .containsExactly("2");
            assertThat(rows(connection, "SELECT * FROM kkref_notification_recovery_permit")).isEqualTo(permitBefore);
            assertThat(rows(connection, "SELECT * FROM kkref_notification_recovery_consumption")).isEqualTo(consumptionBefore);
            System.out.println("S1_COMPAT upgradedDisabled=true validation=true defaultLocations=true ignorePatterns="
                    + java.util.Arrays.toString(flyway.getConfiguration().getIgnoreMigrationPatterns()));
        }
    }
}
