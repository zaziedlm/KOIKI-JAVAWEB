package org.koikifw.runtimeconsumer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.koikifw.cp6fixture.Cp6EntityExposureFixture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({RuntimePostgreSqlTestConfiguration.class, Cp6EntityExposureFixture.Configuration.class})
class RuntimeFoundationConsumerOsivDisabledTest {

    @LocalServerPort
    private int port;

    @Autowired
    private Environment environment;

    private RestTestClient client;

    @BeforeEach
    void createClient() {
        client = RestTestClient.bindToServer()
                .baseUrl("http://127.0.0.1:" + port)
                .build();
    }

    @Test
    void detectsTestOnlyEntityExposureDuringResponseSerialization() {
        assertThat(environment.getProperty("spring.jpa.open-in-view", Boolean.class)).isFalse();

        client.get()
                .uri("/api/1/test-only/entity-exposure")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.code").isEqualTo("KOIKI-HTTP-500");
    }
}
