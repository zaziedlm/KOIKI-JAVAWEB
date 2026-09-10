package org.koikifw.buildsupport.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Tooling-owned PostgreSQL schema for P2-B1 T4 only. */
@TestConfiguration(proxyBeanMethods = false)
class AuditPostgreSqlTestConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer auditPostgreSqlContainer() {
        return new PostgreSQLContainer("postgres:17-alpine")
                .withInitScript("audit-fixture-schema.sql");
    }
}
