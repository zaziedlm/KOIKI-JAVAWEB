package org.koikifw.reference.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.koikifw.reference.expense.domain.event.ExpenseApprovedEvent;
import org.koikifw.reference.notification.adapter.outbound.external.HttpNotificationProviderAdapter;

class NotificationProviderEndpointContractTest {
    @Test
    void rejectsOtherEndpointsAndNeverFollowsRedirects() throws Exception {
        for (String invalid : new String[]{"http://example.invalid:1234/notifications",
                "http://localhost:1234/notifications", "http://user@127.0.0.1:1234/notifications",
                "http://127.0.0.1:1234/notifications?secret=x"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new HttpNotificationProviderAdapter(URI.create(invalid)));
        }
        var accepted = new AtomicInteger();
        var target = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var redirect = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
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
                assertThrows(IllegalStateException.class, () -> adapter.deliver(event));
                assertEquals(0, accepted.get());
            }
        } finally {
            redirect.stop(0);
            target.stop(0);
        }
    }
}
