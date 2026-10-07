package org.koikifw.reference;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.DriverManager;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Non-distributed PostgreSQL 17 fixture for Reference application verification. */
@TestConfiguration(proxyBeanMethods = false)
public class ReferencePostgreSqlTestConfiguration {

    private static final String LIMITS = "koiki.reference.verification.resource-limits.enabled";

    static boolean resourceLimitsEnabled() {
        String value = System.getProperty(LIMITS, "false");
        if (!value.equals("true") && !value.equals("false")) {
            throw new IllegalArgumentException("Invalid verification resource-limits property");
        }
        return value.equals("true");
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgreSqlContainer() {
        if (!resourceLimitsEnabled()) {
            return new PostgreSQLContainer("postgres:17-alpine");
        }
        if (!"4".equals(System.getProperty("spring.datasource.hikari.maximum-pool-size"))
                || !"1".equals(System.getProperty("spring.datasource.hikari.minimum-idle"))) {
            throw new IllegalArgumentException("Limited verification requires pool maximum 4 / minimum idle 1");
        }
        if (Runtime.getRuntime().maxMemory() != 768L * 1024 * 1024) {
            throw new IllegalStateException("Limited verification requires test JVM heap 768 MiB");
        }
        return new PostgreSQLContainer("postgres:17-alpine") {
            @Override
            public void start() {
                super.start();
                try {
                    var host = getContainerInfo().getHostConfig();
                    if (!Long.valueOf(1073741824L).equals(host.getMemory())
                            || !Long.valueOf(1000000000L).equals(host.getNanoCPUs())) {
                        throw new IllegalStateException("DB resource limits were not applied");
                    }
                    try (var connection = DriverManager.getConnection(getJdbcUrl(), getUsername(), getPassword());
                            var statement = connection.createStatement();
                            var result = statement.executeQuery("SHOW max_connections")) {
                        if (!result.next() || result.getInt(1) != 16) {
                            throw new IllegalStateException("DB max_connections limit was not applied");
                        }
                    }
                    System.out.println("S1_RESOURCE dbId=" + getContainerId()
                            + " memory=1073741824 nanoCpus=1000000000 maxConnections=16 heap=805306368");
                } catch (Exception failure) {
                    stop();
                    throw new IllegalStateException("Verification DB limit check failed", failure);
                }
            }
        }.withCommand("postgres", "-c", "max_connections=16")
                .withCreateContainerCmdModifier(command -> command.getHostConfig()
                        .withMemory(1073741824L).withNanoCPUs(1000000000L));
    }

    @Bean
    static BeanPostProcessor verificationPoolLimits() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (resourceLimitsEnabled() && bean instanceof HikariDataSource pool) {
                    if (pool.getMaximumPoolSize() != 4 || pool.getMinimumIdle() != 1) {
                        throw new IllegalStateException("Effective verification pool differs from 4 / 1");
                    }
                    System.out.println("S1_RESOURCE poolMaximum=4 poolMinimumIdle=1");
                }
                return bean;
            }
        };
    }
}
