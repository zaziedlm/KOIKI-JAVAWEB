package org.koikifw.buildsupport.referencee2e;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Tooling-only loopback gate before stub arrival or after its independent receipt commit. */
final class R3ProviderGate implements AutoCloseable {
    enum Position { BEFORE_ARRIVAL, AFTER_RECEIPT }

    private final Position position;
    private final URI stubEndpoint;
    private final HttpServer server;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final CountDownLatch reached = new CountDownLatch(1);
    private final CountDownLatch released = new CountDownLatch(1);
    private final AtomicBoolean abort = new AtomicBoolean();
    private final AtomicReference<Exception> failure = new AtomicReference<>();

    R3ProviderGate(Position position, URI stubEndpoint) throws Exception {
        this.position = position;
        this.stubEndpoint = stubEndpoint;
        server = HttpServer.create(new InetSocketAddress(
                InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), 0), 0);
        server.createContext("/notifications", this::receive);
        server.setExecutor(executor);
        server.start();
    }

    URI endpoint() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/notifications");
    }

    void awaitGate() throws Exception {
        if (!reached.await(4, TimeUnit.SECONDS)) {
            throw new IllegalStateException("R3 provider gate not reached", failure.get());
        }
        if (failure.get() != null) throw new IllegalStateException("R3 provider gate failed", failure.get());
    }

    void abortAfterStop() {
        abort.set(true);
        released.countDown();
    }

    private void receive(HttpExchange exchange) {
        try (exchange) {
            byte[] body = exchange.getRequestBody().readNBytes(4097);
            if (body.length > 4096 || !"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }
            if (position == Position.BEFORE_ARRIVAL) {
                reached.countDown();
                released.await(5, TimeUnit.SECONDS);
                if (abort.get()) return;
            }
            HttpRequest request = HttpRequest.newBuilder(stubEndpoint).timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", required(exchange, "Idempotency-Key"))
                    .header("X-Koiki-Event-Id", required(exchange, "X-Koiki-Event-Id"))
                    .header("X-Payload-SHA256", required(exchange, "X-Payload-SHA256"))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (position == Position.AFTER_RECEIPT) {
                reached.countDown();
                released.await(5, TimeUnit.SECONDS);
                if (abort.get()) return;
            }
            exchange.sendResponseHeaders(response.statusCode(), response.body().length);
            exchange.getResponseBody().write(response.body());
        } catch (Exception exception) {
            failure.compareAndSet(null, exception);
            reached.countDown();
        }
    }

    private static String required(HttpExchange exchange, String name) {
        String value = exchange.getRequestHeaders().getFirst(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("R3 provider header absent");
        return value;
    }

    @Override
    public void close() throws Exception {
        abortAfterStop();
        server.stop(0);
        executor.shutdownNow();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("R3 provider gate thread did not stop");
        }
        client.close();
    }
}
