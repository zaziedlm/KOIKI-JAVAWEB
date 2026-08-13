package org.koikifw.walkingskeleton.tier2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import dev.koiki.walkingskeleton.architecture.KoikiModule;
import dev.koiki.walkingskeleton.architecture.ModuleTier;
import dev.koiki.walkingskeleton.architecture.PersistenceModel;
import dev.koiki.walkingskeleton.architecture.PersistenceTechnology;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class Tier2PracticalityInfrastructureTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void startsApplicationContextWithOsivDisabled() {
        assertThat(applicationContext).isNotNull();
        assertThat(environment.getProperty("spring.jpa.open-in-view", Boolean.class))
                .isFalse();
        assertThat(Runtime.version().feature()).isEqualTo(21);
    }

    @Test
    void declaresBusinessModuleMetadata() throws ClassNotFoundException {
        KoikiModule expense = moduleMetadata(
                "org.koikifw.walkingskeleton.tier2.expense.package-info");
        assertThat(expense.name()).isEqualTo("expense");
        assertThat(expense.tier()).isEqualTo(ModuleTier.RICH);
        assertThat(expense.persistence()).isEqualTo(PersistenceTechnology.JPA);
        assertThat(expense.persistenceModel()).isEqualTo(PersistenceModel.SHARED);

        KoikiModule masterdata = moduleMetadata(
                "org.koikifw.walkingskeleton.tier2.masterdata.package-info");
        assertThat(masterdata.name()).isEqualTo("masterdata");
        assertThat(masterdata.tier()).isEqualTo(ModuleTier.SIMPLE);
        assertThat(masterdata.persistence()).isEqualTo(PersistenceTechnology.JPA);
        assertThat(masterdata.persistenceModel()).isEqualTo(PersistenceModel.SHARED);
    }

    @Test
    void appliesApplicationOwnedFlywayMigration() {
        List<String> tableNames = jdbcTemplate.queryForList(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name LIKE 'ws_%'
                ORDER BY table_name
                """,
                String.class);

        assertThat(tableNames).containsExactly(
                "ws_category",
                "ws_expense_line",
                "ws_expense_request");

        Integer migrationCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE success = TRUE
                  AND version = '1'
                """,
                Integer.class);

        assertThat(migrationCount).isEqualTo(1);
    }

    private static KoikiModule moduleMetadata(String packageInfoClassName)
            throws ClassNotFoundException {
        Package modulePackage = Class.forName(packageInfoClassName).getPackage();
        return modulePackage.getAnnotation(KoikiModule.class);
    }
}
