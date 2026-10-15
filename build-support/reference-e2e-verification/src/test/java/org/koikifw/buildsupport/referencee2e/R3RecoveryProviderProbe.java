package org.koikifw.buildsupport.referencee2e;

import com.sun.net.httpserver.HttpServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

/** Tooling-only endpoint that observes requests from the rejected recovery process. */
final class R3RecoveryProviderProbe implements AutoCloseable {
    private final HttpServer server;
    private final AtomicInteger arrivals = new AtomicInteger();

    R3RecoveryProviderProbe() throws Exception {
        server = HttpServer.create(new InetSocketAddress(
                InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), 0), 0);
        server.createContext("/notifications", exchange -> {
            arrivals.incrementAndGet();
            try (exchange) {
                exchange.sendResponseHeaders(503, -1);
            }
        });
        server.start();
    }

    URI endpoint() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/notifications");
    }

    int arrivalCount() {
        return arrivals.get();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
