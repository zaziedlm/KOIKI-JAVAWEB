package org.koikifw.runtimeconsumer;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.hikari.connection-timeout=1000",
                "spring.datasource.hikari.validation-timeout=500",
                "spring.datasource.hikari.data-source-properties.socketTimeout=1"
        })
@Import(RuntimePostgreSqlTestConfiguration.class)
class RuntimeFoundationConsumerHealthTest {

    private static final Duration POSTGRES_RESTORE_TIMEOUT = Duration.ofSeconds(30);

    @LocalServerPort
    private int port;

    @Autowired
    private Environment environment;

    @Autowired
    private PostgreSQLContainer postgreSqlContainer;

    private RestTestClient client;

    @BeforeEach
    void createClient() {
        client = RestTestClient.bindToServer()
                .baseUrl("http://127.0.0.1:" + port)
                .build();
    }

    @Test
    void exposesSafeHealthComponentsAndClassifiedProbes() {
        assertThat(environment.getProperty("spring.jpa.open-in-view", Boolean.class)).isFalse();

        expectUp("/actuator/health", "db");
        expectUp("/actuator/health/liveness");
        expectUp("/actuator/health/readiness", "db");
    }

    @Test
    void reflectsDatabaseDownAndRestoresWithoutAffectingLiveness() throws Exception {
        expectUp("/actuator/health", "db");
        String containerId = postgreSqlContainer.getContainerId();

        try {
            postgreSqlContainer.getDockerClient().pauseContainerCmd(containerId).exec();

            expectDown("/actuator/health", "db");
            expectDown("/actuator/health/readiness", "db");
            expectUp("/actuator/health/liveness");
        } finally {
            postgreSqlContainer.getDockerClient().unpauseContainerCmd(containerId).exec();
            awaitPostgreSql();
        }

        expectUp("/actuator/health", "db");
        expectUp("/actuator/health/readiness", "db");
    }

    private void expectUp(String path, String component) {
        client.get()
                .uri(path)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.components." + component + ".status").isEqualTo("UP")
                .consumeWith(result -> assertSafeHealthBody(result.getResponseBody()));
    }

    private void expectUp(String path) {
        client.get()
                .uri(path)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP")
                .consumeWith(result -> assertSafeHealthBody(result.getResponseBody()));
    }

    private void expectDown(String path, String component) {
        client.get()
                .uri(path)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.status").isEqualTo("DOWN")
                .jsonPath("$.components." + component + ".status").isEqualTo("DOWN")
                .consumeWith(result -> assertSafeHealthBody(result.getResponseBody()));
    }

    private void assertSafeHealthBody(byte @Nullable [] responseBody) {
        String body = new String(Objects.requireNonNull(responseBody), StandardCharsets.UTF_8);
        assertThat(body)
                .doesNotContain(
                        postgreSqlContainer.getJdbcUrl(),
                        postgreSqlContainer.getUsername(),
                        postgreSqlContainer.getPassword(),
                        "jdbc:",
                        "SQLException",
                        "Connection refused",
                        "stackTrace");
    }

    private void awaitPostgreSql() throws Exception {
        long deadline = System.nanoTime() + POSTGRES_RESTORE_TIMEOUT.toNanos();
        SQLException lastFailure = null;
        while (System.nanoTime() < deadline) {
            try (Connection ignored = DriverManager.getConnection(
                    postgreSqlContainer.getJdbcUrl(),
                    postgreSqlContainer.getUsername(),
                    postgreSqlContainer.getPassword())) {
                return;
            } catch (SQLException exception) {
                lastFailure = exception;
                Thread.sleep(250);
            }
        }
        throw new IllegalStateException("PostgreSQL did not recover within the test timeout", lastFailure);
    }
}
