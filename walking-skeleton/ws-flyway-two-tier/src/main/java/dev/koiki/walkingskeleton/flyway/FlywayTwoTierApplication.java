package dev.koiki.walkingskeleton.flyway;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootApplication
@EnableConfigurationProperties(FlywayValidationProperties.class)
public class FlywayTwoTierApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlywayTwoTierApplication.class, args);
    }

    @Bean
    FlywayMigrationStrategy koikiBeforeCustomer(DataSource dataSource) {
        return customerFlyway -> {
            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration/koiki")
                    .table("koiki_flyway_history")
                    .load()
                    .migrate();
            customerFlyway.migrate();
        };
    }

    @Bean
    CommandLineRunner verifyIndependentHistories(
            JdbcClient jdbcClient,
            FlywayValidationProperties validationProperties) {
        return args -> {
            int koikiVersion = currentVersion(jdbcClient, "koiki_flyway_history");
            int customerVersion = currentVersion(jdbcClient, "flyway_schema_history");

            if (koikiVersion != validationProperties.expectedKoikiVersion()) {
                throw new IllegalStateException(
                        "Expected KOIKI Flyway version " + validationProperties.expectedKoikiVersion()
                                + " but found " + koikiVersion);
            }
            if (customerVersion != 5) {
                throw new IllegalStateException(
                        "Expected Customer Flyway version 5 but found " + customerVersion);
            }

            System.out.printf(
                    "KOIKI_FLYWAY_VALIDATED koiki=%d customer=%d histories=independent%n",
                    koikiVersion,
                    customerVersion);
        };
    }

    private static int currentVersion(JdbcClient jdbcClient, String historyTable) {
        return jdbcClient.sql(
                        "SELECT MAX(CAST(version AS INTEGER)) FROM " + historyTable
                                + " WHERE success = TRUE")
                .query(Integer.class)
                .single();
    }
}
