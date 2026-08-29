package org.koikifw.starter.data.internal;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;

/** Runs KOIKI-owned migrations before Spring Boot's Customer-owned Flyway. */
@AutoConfiguration(
        afterName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        beforeName = "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration")
@ConditionalOnClass({Flyway.class, FlywayMigrationStrategy.class})
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(prefix = "koiki.data", name = "enabled", matchIfMissing = true)
@ConditionalOnProperty(prefix = "koiki.data.flyway", name = "enabled", matchIfMissing = true)
public class KoikiDataFlywayAutoConfiguration {

    static final String KOIKI_LOCATION = "classpath:db/migration/koiki";
    static final String KOIKI_HISTORY_TABLE = "koiki_flyway_history";

    @Bean
    @ConditionalOnMissingBean(FlywayMigrationStrategy.class)
    FlywayMigrationStrategy koikiFlywayMigrationStrategy(DataSource dataSource) {
        return customerFlyway -> {
            Flyway koikiFlyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations(KOIKI_LOCATION)
                    .table(KOIKI_HISTORY_TABLE)
                    .baselineOnMigrate(true)
                    .baselineVersion(MigrationVersion.fromVersion("0"))
                    .load();
            koikiFlyway.migrate();
            customerFlyway.migrate();
        };
    }
}
