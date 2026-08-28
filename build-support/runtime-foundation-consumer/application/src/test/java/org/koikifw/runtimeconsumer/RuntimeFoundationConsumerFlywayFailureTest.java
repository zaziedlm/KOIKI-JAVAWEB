package org.koikifw.runtimeconsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(RuntimePostgreSqlTestConfiguration.class)
class RuntimeFoundationConsumerFlywayFailureTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void rejectsMixedOwnerLocationsWithDuplicateVersions() {
        String schema = newSchema("mixed_owner");
        try {
            Flyway mixedOwner = flyway(
                    schema,
                    "mixed_owner_history",
                    "classpath:db/cp4/mixed/koiki",
                    "classpath:db/cp4/mixed/customer");

            assertThatThrownBy(mixedOwner::migrate)
                    .isInstanceOf(FlywayException.class)
                    .hasMessageContaining("version 1");
        } finally {
            dropSchema(schema);
        }
    }

    @Test
    void detectsCustomerFirstFailureAndRestoresByRunningKoikiFirst() {
        String schema = newSchema("reverse_order");
        try {
            Flyway customer = flyway(
                    schema,
                    "flyway_schema_history",
                    "classpath:db/cp4/reverse/customer");

            assertThatThrownBy(customer::migrate)
                    .isInstanceOf(FlywayException.class)
                    .hasMessageContaining("koiki_order_probe");

            dropSchema(schema);
            createSchema(schema);

            Flyway koiki = flyway(
                    schema,
                    "koiki_flyway_history",
                    "classpath:db/cp4/reverse/koiki");
            customer = flyway(
                    schema,
                    "flyway_schema_history",
                    "classpath:db/cp4/reverse/customer");

            koiki.migrate();
            customer.migrate();

            Integer restoredMarker = jdbcClient.sql(
                            "select marker from " + schema + ".kkbiz_order_probe where id = 1")
                    .query(Integer.class)
                    .single();
            assertThat(restoredMarker).isEqualTo(1);
        } finally {
            dropSchema(schema);
        }
    }

    @Test
    void appliesLaterKoikiVersionIndependentlyOfCustomerVersionSequence() {
        String schema = newSchema("later_koiki");
        try {
            Flyway koikiInitial = flyway(
                    schema,
                    "koiki_flyway_history",
                    "classpath:db/cp4/independent/koiki-initial");
            Flyway customer = flyway(
                    schema,
                    "flyway_schema_history",
                    "classpath:db/cp4/independent/customer");

            koikiInitial.migrate();
            customer.migrate();

            Flyway koikiComplete = flyway(
                    schema,
                    "koiki_flyway_history",
                    "classpath:db/cp4/independent/koiki-complete");
            koikiComplete.migrate();

            assertThat(currentVersion(koikiComplete)).isEqualTo("2");
            assertThat(currentVersion(customer)).isEqualTo("5");
            Integer koikiRows = jdbcClient.sql(
                            "select count(*) from " + schema + ".koiki_independent_probe")
                    .query(Integer.class)
                    .single();
            Integer customerRows = jdbcClient.sql(
                            "select count(*) from " + schema + ".kkbiz_independent_probe")
                    .query(Integer.class)
                    .single();
            assertThat(koikiRows).isEqualTo(2);
            assertThat(customerRows).isEqualTo(2);
        } finally {
            dropSchema(schema);
        }
    }

    private Flyway flyway(String schema, String historyTable, String... locations) {
        return Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .defaultSchema(schema)
                .locations(locations)
                .table(historyTable)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
    }

    private String newSchema(String scenario) {
        String schema = "cp4_" + scenario + "_" + UUID.randomUUID().toString().replace("-", "");
        createSchema(schema);
        return schema;
    }

    private void createSchema(String schema) {
        jdbcClient.sql("create schema " + schema).update();
    }

    private void dropSchema(String schema) {
        jdbcClient.sql("drop schema if exists " + schema + " cascade").update();
    }

    private String currentVersion(Flyway flyway) {
        return flyway.info().current().getVersion().getVersion();
    }
}
