package com.example.koikiconsumer.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"debug=false", "logging.level.root=INFO"})
class SecurityFoundationConsumerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private List<SecurityFilterChain> securityFilterChains;

    private RestTestClient client;

    @BeforeEach
    void createClient() {
        client = RestTestClient.bindToServer()
                .baseUrl("http://127.0.0.1:" + port)
                .build();
    }

    @Test
    void composesCustomerChainWithFrameworkFallback() {
        assertThat(securityFilterChains).hasSize(2);

        client.get()
                .uri("/consumer/public")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectBody(String.class).isEqualTo("consumer-public-ok");
        client.get()
                .uri("/consumer/private")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();
        client.get()
                .uri("/consumer/unmatched")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();
    }
}
