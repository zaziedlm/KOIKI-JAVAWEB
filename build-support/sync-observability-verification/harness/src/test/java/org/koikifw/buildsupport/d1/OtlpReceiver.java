package org.koikifw.buildsupport.d1;

import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** A bounded OTLP/protobuf test endpoint, not a production Collector. */
final class OtlpReceiver implements AutoCloseable {
    record Received(Span span, String service, int mode) { }
    final List<Received> spans = new CopyOnWriteArrayList<>();
    final AtomicInteger requests = new AtomicInteger();
    final AtomicInteger mode = new AtomicInteger(); // 0 success, 1 503, 2 two-second delay
    final AtomicReference<String> fault = new AtomicReference<>();
    private final AtomicInteger decoded = new AtomicInteger();
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(16));
    private final HttpServer server;
    private final Path evidence;
    private volatile boolean closed;

    OtlpReceiver(Path evidence) throws IOException {
        this.evidence = evidence;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 8);
        server.setExecutor(executor);
        server.createContext("/v1/traces", exchange -> {
            try (exchange) {
                int n = requests.incrementAndGet();
                if (n > 100) { throw new IOException("OTLP request cap exceeded"); }
                if (!"POST".equals(exchange.getRequestMethod())
                        || !"application/x-protobuf".equals(exchange.getRequestHeaders().getFirst("Content-Type"))) {
                    throw new IOException("Unexpected OTLP method or encoding");
                }
                byte[] body = exchange.getRequestBody().readNBytes(1_048_577);
                if (body.length > 1_048_576) { throw new IOException("OTLP body cap exceeded"); }
                var request = ExportTraceServiceRequest.parseFrom(body);
                int selected = mode.get();
                for (var resource : request.getResourceSpansList()) {
                    String service = resource.getResource().getAttributesList().stream()
                            .filter(a -> a.getKey().equals("service.name"))
                            .map(a -> a.getValue().getStringValue()).findFirst().orElse("");
                    for (var scope : resource.getScopeSpansList()) {
                        for (Span span : scope.getSpansList()) {
                            if (decoded.incrementAndGet() > 500) { throw new IOException("Span cap exceeded"); }
                            spans.add(new Received(span, service, selected));
                        }
                    }
                }
                // Reject synthetic secrets before persisting payloads.
                String text = request.toString();
                if (text.contains(D1ConnectionTest.AUTH_CANARY) || text.contains(D1ConnectionTest.BODY_CANARY)) {
                    throw new IOException("Synthetic canary leaked into telemetry");
                }
                Files.write(evidence.resolve("otlp-" + n + ".bin"), body);
                Files.writeString(evidence.resolve("otlp-" + n + ".txt"), text, StandardCharsets.UTF_8);
                if (selected == 2) {
                    try { Thread.sleep(2000); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                }
                try {
                    exchange.sendResponseHeaders(selected == 1 ? 503 : 200, -1);
                } catch (IOException ex) {
                    // The client is expected to disconnect after its 500ms timeout in delay mode.
                    if (selected != 2 && !closed) { throw ex; }
                }
            } catch (Exception ex) {
                if (!closed) { fault.compareAndSet(null, ex.getClass().getSimpleName() + ": " + ex.getMessage()); }
            }
        });
        server.start();
    }
    int port() { return server.getAddress().getPort(); }
    void healthy() {
        if (fault.get() != null) { throw new AssertionError("OTLP receiver failed: " + fault.get()); }
    }
    @Override public void close() throws IOException {
        closed = true;
        server.stop(0);
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) { throw new IOException("Receiver executor remained"); }
        } catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IOException(ex); }
        Files.writeString(evidence.resolve("receiver-cleanup.txt"), "server stopped; executor terminated\n");
    }
}
