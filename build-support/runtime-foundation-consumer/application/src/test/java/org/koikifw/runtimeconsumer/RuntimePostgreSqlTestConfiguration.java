package org.koikifw.runtimeconsumer;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Customer-owned PostgreSQL 17 fixture assembled through KOIKI Testing Support. */
@TestConfiguration(proxyBeanMethods = false)
class RuntimePostgreSqlTestConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgreSqlContainer() {
        return new PostgreSQLContainer("postgres:17-alpine");
    }
}
