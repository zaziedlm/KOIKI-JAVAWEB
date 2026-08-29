package org.koikifw.buildsupport.internal.runtimefoundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.koikifw.starter.data.internal.KoikiDataFlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DataStarterAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KoikiDataFlywayAutoConfiguration.class))
            .withBean(DataSource.class, () -> mock(DataSource.class));

    @Test
    void contributesKoikiBeforeCustomerMigrationStrategyByDefault() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(FlywayMigrationStrategy.class)
                .hasBean("koikiFlywayMigrationStrategy"));
    }

    @Test
    void backsOffWhenDataRuntimeIsDisabled() {
        contextRunner
                .withPropertyValues("koiki.data.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(FlywayMigrationStrategy.class));
    }

    @Test
    void backsOffWhenFlywayIntegrationIsDisabled() {
        contextRunner
                .withPropertyValues("koiki.data.flyway.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(FlywayMigrationStrategy.class));
    }

    @Test
    void preservesApplicationOwnedMigrationStrategy() {
        FlywayMigrationStrategy applicationStrategy = flyway -> {
        };

        contextRunner
                .withBean("applicationFlywayMigrationStrategy", FlywayMigrationStrategy.class,
                        () -> applicationStrategy)
                .run(context -> assertThat(context)
                        .hasSingleBean(FlywayMigrationStrategy.class)
                        .getBean(FlywayMigrationStrategy.class)
                        .isSameAs(applicationStrategy));
    }
}
