package org.koikifw.runtimeconsumer;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RuntimeFoundationConsumerHttpTest {

    @LocalServerPort
    private int port;

    private RestTestClient client;

    @BeforeEach
    void createClient() {
        client = RestTestClient.bindToServer()
                .baseUrl("http://127.0.0.1:" + port)
                .build();
    }

    @Test
    void createsWorkItemThroughVersionedHttpAndUseCase() {
        client.post()
                .uri("/api/1/work-items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("label", "customer-like"))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("Location", "/api/1/work-items/[0-9a-f-]+")
                .expectBody()
                .jsonPath("$.id").exists();
    }

    @Test
    void rejectsUnsupportedPathVersion() {
        client.post()
                .uri("/api/2/work-items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("label", "unsupported"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void rejectsMissingPathVersion() {
        client.post()
                .uri("/api/work-items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("label", "missing"))
                .exchange()
                .expectStatus().isNotFound();
    }
}
