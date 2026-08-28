package org.koikifw.runtimeconsumer;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(RuntimeFoundationConsumerHttpTest.RuntimeFailureController.class)
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
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.type").isEqualTo("about:blank")
                .jsonPath("$.title").isEqualTo("Bad Request")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.code").isEqualTo("KOIKI-HTTP-400")
                .jsonPath("$.instance").isEqualTo("/api/2/work-items");
    }

    @Test
    void rejectsMissingPathVersion() {
        client.post()
                .uri("/api/work-items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("label", "missing"))
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.code").isEqualTo("KOIKI-HTTP-404")
                .jsonPath("$.instance").isEqualTo("/api/work-items");
    }

    @Test
    void returnsValidationProblemDetailsWithoutRejectedValues() {
        client.post()
                .uri("/api/1/work-items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of())
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo("Request validation failed.")
                .jsonPath("$.code").isEqualTo("KOIKI-VALIDATION-001")
                .jsonPath("$.violations[0].field").isEqualTo("label")
                .consumeWith(result -> assertThat(body(result.getResponseBody()))
                        .doesNotContain("rejectedValue"));
    }

    @Test
    void returnsSafeProblemDetailsForMalformedAndUnknownJson() {
        client.post()
                .uri("/api/1/work-items")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"label\":")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("KOIKI-JSON-001")
                .jsonPath("$.detail").isEqualTo("Request body is not valid JSON.")
                .consumeWith(result -> assertThat(body(result.getResponseBody()))
                        .doesNotContain("Unexpected end-of-input"));

        client.post()
                .uri("/api/1/work-items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("label", "valid", "internalProbe", true))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("KOIKI-JSON-001")
                .consumeWith(result -> assertThat(body(result.getResponseBody()))
                        .doesNotContain("internalProbe"));
    }

    @Test
    void returnsSafeProblemDetailsForUnhandledProcessingFailure() {
        client.get()
                .uri("/api/1/runtime-failure")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(500)
                .jsonPath("$.detail").isEqualTo("An unexpected error occurred.")
                .jsonPath("$.code").isEqualTo("KOIKI-INTERNAL-001")
                .jsonPath("$.instance").isEqualTo("/api/1/runtime-failure")
                .consumeWith(result -> assertThat(body(result.getResponseBody()))
                        .doesNotContain("consumer-secret", "IllegalStateException"));
    }

    private static String body(byte @Nullable [] responseBody) {
        return new String(Objects.requireNonNull(responseBody), StandardCharsets.UTF_8);
    }

    @RestController
    @RequestMapping("/api/{version}/runtime-failure")
    static class RuntimeFailureController {

        @GetMapping(version = "1")
        void fail() {
            throw new IllegalStateException("consumer-secret");
        }
    }
}
