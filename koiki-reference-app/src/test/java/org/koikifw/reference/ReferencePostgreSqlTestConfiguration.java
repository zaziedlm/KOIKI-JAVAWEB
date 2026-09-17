package org.koikifw.reference;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Non-distributed PostgreSQL 17 fixture for Reference application verification. */
@TestConfiguration(proxyBeanMethods = false)
public class ReferencePostgreSqlTestConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgreSqlContainer() {
        return new PostgreSQLContainer("postgres:17-alpine");
    }
}
