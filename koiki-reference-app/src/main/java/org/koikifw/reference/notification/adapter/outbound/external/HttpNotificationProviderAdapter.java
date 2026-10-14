package org.koikifw.reference.notification.adapter.outbound.external;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.koikifw.reference.expense.domain.event.ExpenseApprovedEvent;
import org.koikifw.reference.notification.application.port.outbound.NotificationProviderPort;

/** R1 local fixture adapter. Does not follow redirects or support production endpoints. */
public final class HttpNotificationProviderAdapter implements NotificationProviderPort, AutoCloseable {
    private final URI endpoint;
    private final HttpClient client;

    public HttpNotificationProviderAdapter(URI endpoint) {
        String host = endpoint.getHost();
        if (!"http".equals(endpoint.getScheme())
                || !("127.0.0.1".equals(host) || "[::1]".equals(host) || "::1".equals(host))
                || endpoint.getPort() < 1 || endpoint.getPort() > 65535
                || endpoint.getRawUserInfo() != null || endpoint.getRawQuery() != null
                || endpoint.getRawFragment() != null || !"/notifications".equals(endpoint.getPath())) {
            throw new IllegalArgumentException("R1 provider requires a numeric loopback notification endpoint");
        }
        this.endpoint = endpoint;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @Override
    public Receipt deliver(ExpenseApprovedEvent event) {
        var request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", event.notificationKey())
                .header("X-Koiki-Event-Id", event.eventId().toString())
                .header("X-Recipient-Id", event.applicantUserId().toString())
                .header("X-Payload-SHA256", event.payloadSha256())
                .POST(HttpRequest.BodyPublishers.ofString(event.canonicalPayload())).build();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (var body = response.body()) {
                if (response.statusCode() != 200 && response.statusCode() != 201) {
                    var kind = response.statusCode() == 409 || response.statusCode() == 410
                            ? DeliveryFailureKind.EXPLICIT_REJECTION : DeliveryFailureKind.UNKNOWN;
                    throw new DeliveryFailure(kind, "Local notification provider did not acknowledge");
                }
                byte[] bytes = body.readNBytes(257);
                if (bytes.length > 256) {
                    throw new DeliveryFailure(DeliveryFailureKind.UNKNOWN,
                            "Invalid provider acknowledgement size");
                }
                try {
                    String acknowledgement = new String(bytes, StandardCharsets.UTF_8).trim();
                    UUID acceptanceId = UUID.fromString(acknowledgement);
                    if (!acceptanceId.toString().equals(acknowledgement)) {
                        throw new IllegalArgumentException("Noncanonical acknowledgement");
                    }
                    return new Receipt(acceptanceId);
                } catch (IllegalArgumentException exception) {
                    throw new DeliveryFailure(DeliveryFailureKind.UNKNOWN,
                            "Invalid provider acknowledgement", exception);
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DeliveryFailure(DeliveryFailureKind.UNKNOWN, "Local provider operation interrupted", exception);
        } catch (IOException exception) {
            throw new DeliveryFailure(DeliveryFailureKind.UNKNOWN, "Local provider operation failed", exception);
        }
    }

    @Override
    public Inspection inspect(ExpenseApprovedEvent event) {
        var request = HttpRequest.newBuilder(endpoint.resolve("/notifications/receipt"))
                .timeout(Duration.ofSeconds(5))
                .header("Idempotency-Key", event.notificationKey())
                .header("X-Koiki-Event-Id", event.eventId().toString())
                .header("X-Recipient-Id", event.applicantUserId().toString())
                .header("X-Payload-SHA256", event.payloadSha256())
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (var body = response.body()) {
                byte[] bytes = body.readNBytes(257);
                if (bytes.length > 256) return unknown();
                String value = new String(bytes, StandardCharsets.UTF_8).trim();
                if (response.statusCode() == 200) {
                    UUID receipt = UUID.fromString(value);
                    if (!receipt.toString().equals(value)) return unknown();
                    return new Inspection(InspectionState.ACCEPTED_MATCH, Optional.of(receipt), Optional.empty());
                }
                if (response.statusCode() == 404) {
                    return new Inspection(InspectionState.NOT_FOUND_WITHIN_VALID_WINDOW, Optional.empty(),
                            Optional.of(Instant.parse(value)));
                }
                if (response.statusCode() == 409) {
                    return new Inspection(InspectionState.CONFLICT, Optional.empty(), Optional.empty());
                }
                if (response.statusCode() == 410) {
                    return new Inspection(InspectionState.EXPIRED, Optional.empty(), Optional.empty());
                }
                return unknown();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return unknown();
        } catch (IOException | IllegalArgumentException exception) {
            return unknown();
        }
    }

    private static Inspection unknown() {
        return new Inspection(InspectionState.UNKNOWN, Optional.empty(), Optional.empty());
    }

    @Override
    public void close() {
        client.close();
    }
}
