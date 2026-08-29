package org.koikifw.runtimeconsumer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.TaskDecorator;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "koiki.environment=acceptance",
            "spring.task.execution.pool.core-size=1",
            "spring.task.execution.pool.max-size=1",
            "spring.task.execution.pool.queue-capacity=8"
        })
@Import({RuntimePostgreSqlTestConfiguration.class,
        RuntimeFoundationConsumerObservabilityTest.CustomerTaskDecoratorConfiguration.class})
class RuntimeFoundationConsumerObservabilityTest {

    @LocalServerPort
    private int port;

    @org.springframework.beans.factory.annotation.Autowired
    private JsonMapper jsonMapper;

    private RestTestClient client;

    @BeforeEach
    void createClient() {
        client = RestTestClient.bindToServer()
                .baseUrl("http://127.0.0.1:" + port)
                .build();
    }

    @Test
    void keepsCorrelationIdInStructuredLogAcrossAsyncBoundary(CapturedOutput output) {
        UUID id = UUID.randomUUID();

        client.post()
                .uri("/api/1/work-items/{id}/process", id)
                .header("X-Request-ID", "cp5-request-01")
                .exchange()
                .expectStatus().isAccepted()
                .expectHeader().valueEquals("X-Request-ID", "cp5-request-01")
                .expectBody()
                .jsonPath("$.id").isEqualTo(id.toString())
                .jsonPath("$.result").isEqualTo("accepted");

        JsonNode log = asyncLogs(output).getLast();
        assertThat(log.path("timestamp").asText()).isNotBlank();
        assertThat(log.path("level").asText()).isEqualTo("INFO");
        assertThat(log.path("service").asText()).isEqualTo("runtime-foundation-consumer");
        assertThat(log.path("environment").asText()).isEqualTo("acceptance");
        assertThat(log.path("requestId").asText()).isEqualTo("cp5-request-01");
        assertThat(log.path("operation").asText()).isEqualTo("processWorkItemAsync");
        assertThat(log.path("result").asText()).isEqualTo("success");
        assertThat(log.path("customerDecorator").asText()).isEqualTo("applied");
    }

    @Test
    void doesNotLeakCorrelationIdWhenAsyncThreadIsReused(CapturedOutput output) {
        client.post()
                .uri("/api/1/work-items/{id}/process", UUID.randomUUID())
                .header("X-Request-ID", "cp5-reuse-first")
                .exchange()
                .expectStatus().isAccepted();

        AtomicReference<String> generatedRequestId = new AtomicReference<>();
        client.post()
                .uri("/api/1/work-items/{id}/process", UUID.randomUUID())
                .exchange()
                .expectStatus().isAccepted()
                .expectHeader().value("X-Request-ID", generatedRequestId::set);

        List<JsonNode> logs = asyncLogs(output);
        JsonNode first = logs.get(logs.size() - 2);
        JsonNode second = logs.getLast();
        assertThat(first.path("requestId").asText()).isEqualTo("cp5-reuse-first");
        assertThat(second.path("requestId").asText())
                .isEqualTo(generatedRequestId.get())
                .isNotEqualTo("cp5-reuse-first")
                .matches("[0-9a-f-]{36}");
        assertThat(second.path("thread_name").asText())
                .isEqualTo(first.path("thread_name").asText());
    }

    private List<JsonNode> asyncLogs(CapturedOutput output) {
        return output.getOut().lines()
                .filter(line -> line.startsWith("{"))
                .map(jsonMapper::readTree)
                .filter(Objects::nonNull)
                .filter(node -> "work item async processed".equals(node.path("message").asText()))
                .toList();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CustomerTaskDecoratorConfiguration {

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE + 200)
        TaskDecorator customerTaskDecorator() {
            return runnable -> () -> {
                MDC.put("customerDecorator", "applied");
                try {
                    runnable.run();
                } finally {
                    MDC.remove("customerDecorator");
                }
            };
        }
    }
}
