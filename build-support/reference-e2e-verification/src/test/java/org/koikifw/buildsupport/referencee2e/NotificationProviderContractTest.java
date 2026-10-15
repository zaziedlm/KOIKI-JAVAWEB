package org.koikifw.buildsupport.referencee2e;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.postgresql.PostgreSQLContainer;

@EnabledIfSystemProperty(named="koiki.reference.async.verification.enabled", matches="true")
class NotificationProviderContractTest {
    @Test
    void reusesTheOriginalReceiptForIdenticalRequests() throws Exception {
        try (var db = postgres()) {
            db.start();
            try (var stub = new LocalNotificationProviderStub(db); var client = HttpClient.newHttpClient()) {
                String key = "expense-approved:v1:" + UUID.randomUUID() + ":3:applicant";
                String event = UUID.randomUUID().toString();
                var first = request(client, stub, key, event, "{\"amount\":1200}");
                var same = request(client, stub, key, event, "{\"amount\":1200}");
                assertEquals(201, first.statusCode());
                assertEquals(200, same.statusCode());
                assertEquals(first.body(), same.body());
                assertEquals(1, stub.receiptCount());
            }
        }
    }

    @Test
    void rejectsDifferentPayloadWithoutChangingTheReceipt() throws Exception {
        try (var db = postgres()) {
            db.start();
            try (var stub = new LocalNotificationProviderStub(db); var client = HttpClient.newHttpClient()) {
                String key = "expense-approved:v1:" + UUID.randomUUID() + ":3:applicant";
                String event = UUID.randomUUID().toString();
                var first = request(client, stub, key, event, "{\"amount\":1200}");
                var conflict = request(client, stub, key, event, "{\"amount\":1300}");
                assertEquals(201, first.statusCode());
                assertEquals(409, conflict.statusCode());
                assertEquals(first.body(), stub.receipt(key));
                assertEquals(1, stub.receiptCount());
            }
        }
    }

    private static HttpResponse<String> request(HttpClient client, LocalNotificationProviderStub stub,
            String key, String event, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(stub.endpoint())
                .timeout(Duration.ofSeconds(5))
                .header("Idempotency-Key", key).header("X-Koiki-Event-Id", event)
                .header("X-Payload-SHA256", LocalNotificationProviderStub.sha256(body))
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    static PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17-alpine")
                .withCommand("postgres", "-c", "max_connections=16", "-c", "statement_timeout=10s", "-c", "lock_timeout=10s")
                .withCreateContainerCmdModifier(command -> Objects.requireNonNull(command.getHostConfig())
                        .withMemory(1073741824L).withNanoCPUs(1000000000L));
    }
}
