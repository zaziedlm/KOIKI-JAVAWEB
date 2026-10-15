package org.koikifw.reference.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.koikifw.reference.expense.domain.event.ExpenseApprovedEvent;
import org.koikifw.reference.notification.adapter.outbound.external.HttpNotificationProviderAdapter;
import org.koikifw.reference.notification.application.port.outbound.NotificationProviderPort.InspectionState;
import org.koikifw.reference.notification.application.port.outbound.NotificationProviderPort.DeliveryFailure;
import org.koikifw.reference.notification.application.port.outbound.NotificationProviderPort.DeliveryFailureKind;

class NotificationProviderEndpointContractTest {
    @Test
    void acceptedPostReturnsReceiptAndCarriesLogicalRecipient() throws Exception {
        var server = HttpServer.create(new InetSocketAddress(
                InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), 0), 0);
        var recipient = new AtomicReference<String>();
        UUID acceptance = UUID.randomUUID();
        server.createContext("/notifications", exchange -> {
            recipient.set(exchange.getRequestHeaders().getFirst("X-Recipient-Id"));
            exchange.getRequestBody().readAllBytes();
            byte[] body = acceptance.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(201, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try (var adapter = new HttpNotificationProviderAdapter(URI.create(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/notifications"))) {
            var event = new ExpenseApprovedEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 3, 1200);
            assertEquals(acceptance, adapter.deliver(event).acceptanceId());
            assertEquals(event.applicantUserId().toString(), recipient.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void receiptQuerySeparatesAcceptedValidAbsentAndUnknown() throws Exception {
        var server = HttpServer.create(new InetSocketAddress(
                InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), 0), 0);
        var status = new AtomicInteger(200);
        UUID acceptance = UUID.randomUUID();
        Instant until = Instant.now().plusSeconds(300);
        server.createContext("/notifications/receipt", exchange -> {
            int code = status.get();
            String value = switch (code) {
                case 200 -> acceptance.toString();
                case 404 -> until.toString();
                default -> "unavailable";
            };
            byte[] body = value.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try (var adapter = new HttpNotificationProviderAdapter(URI.create(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/notifications"))) {
            var event = new ExpenseApprovedEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 3, 1200);
            assertEquals(InspectionState.ACCEPTED_MATCH, adapter.inspect(event).state());
            assertEquals(acceptance, adapter.inspect(event).acceptanceId().orElseThrow());
            status.set(404);
            var absent = adapter.inspect(event);
            assertEquals(InspectionState.NOT_FOUND_WITHIN_VALID_WINDOW, absent.state());
            assertEquals(until, absent.validUntil().orElseThrow());
            status.set(503);
            assertEquals(InspectionState.UNKNOWN, adapter.inspect(event).state());
            assertTrue(adapter.inspect(event).acceptanceId().isEmpty());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsOtherEndpointsAndNeverFollowsRedirects() throws Exception {
        for (String invalid : new String[]{"http://example.invalid:1234/notifications",
                "http://localhost:1234/notifications", "http://user@127.0.0.1:1234/notifications",
                "http://127.0.0.1:1234/notifications?secret=x"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new HttpNotificationProviderAdapter(URI.create(invalid)));
        }
        var accepted = new AtomicInteger();
        var loopbackV4 = InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
        var target = HttpServer.create(new InetSocketAddress(loopbackV4, 0), 0);
        var redirect = HttpServer.create(new InetSocketAddress(loopbackV4, 0), 0);
        target.createContext("/notifications", exchange -> {
            accepted.incrementAndGet();
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        redirect.createContext("/notifications", exchange -> {
            exchange.getResponseHeaders().set("Location", "http://127.0.0.1:"
                    + target.getAddress().getPort() + "/notifications");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        try {
            target.start();
            redirect.start();
            try (var adapter = new HttpNotificationProviderAdapter(URI.create(
                    "http://127.0.0.1:" + redirect.getAddress().getPort() + "/notifications"))) {
                var event = new ExpenseApprovedEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 3, 1200);
                assertEquals(DeliveryFailureKind.UNKNOWN,
                        assertThrows(DeliveryFailure.class, () -> adapter.deliver(event)).kind());
                assertEquals(0, accepted.get());
            }
        } finally {
            redirect.stop(0);
            target.stop(0);
        }
    }
}
