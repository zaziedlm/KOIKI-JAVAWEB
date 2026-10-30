package org.koikifw.buildsupport.d1;

import static org.junit.jupiter.api.Assertions.*;

import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** One bounded execution, three sequential children and ten logical checkpoints. */
class D1ConnectionTest {
    static final String AUTH_CANARY = "D1-SYNTHETIC-AUTH-DO-NOT-LOG";
    static final String BODY_CANARY = "D1-SYNTHETIC-BODY-DO-NOT-LOG";
    private static final JsonMapper JSON = new JsonMapper();
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicInteger markers = new AtomicInteger();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
    private Path evidence;
    private boolean virtual;
    private OtlpReceiver sink;
    private int checkpoints;

    @Test @Timeout(300)
    void packagedConsumerExportsRealOtlp() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(Boolean.getBoolean("koiki.d1.enabled"));
        Path manifest = Path.of(System.getProperty("koiki.d1.manifest")).toAbsolutePath();
        JsonNode identity = JSON.readTree(Files.readString(manifest));
        Path jar = Path.of(identity.path("jar").asText());
        evidence = manifest.getParent().resolve("evidence");
        Files.createDirectory(evidence);
        String initialHash = sha(jar);
        assertEquals(identity.path("jarSha256").asText(), initialHash);
        String owner = identity.path("owner").asText();
        assertTrue(owner.matches("[0-9a-f]{32}"));
        String java21 = System.getenv("JAVA21_HOME");
        String java25 = System.getenv("JAVA25_HOME");
        assertNotNull(java21); assertNotNull(java25);
        boolean complete = false;
        try {
            sink = new OtlpReceiver(evidence);
            try (D1Process app = new D1Process(Path.of(java21, "bin/java"), jar, evidence,
                    "disabled", sink.port(), false, false, owner)) {
                virtual = false;
                assertTrue(app.ready.getProperty("java").startsWith("21."));
                request(app, false, true, 200, false);
                Thread.sleep(3000);
                assertEquals(0, sink.requests.get());
                sink.healthy(); mark("O04");
            }
            try (D1Process app = new D1Process(Path.of(java21, "bin/java"), jar, evidence,
                    "enabled", sink.port(), true, false, owner)) {
                virtual = false;
                verifyNormal(app); mark("O01");
                JsonNode failure = request(app, true, true, 500, true);
                var span = awaitSpan(failure);
                // The pinned Micrometer convention tags a returned HTTP500 without a Java error.
                // It does not set OTel span ERROR unless Observation.Context carries an error.
                assertEquals(Status.StatusCode.STATUS_CODE_UNSET, span.span().getStatus().getCode());
                assertTrue(span.span().getAttributesList().stream().anyMatch(a ->
                        a.getKey().equals("status") && a.getValue().getStringValue().equals("500")));
                assertTrue(span.span().getAttributesList().stream().anyMatch(a ->
                        a.getKey().equals("outcome") && a.getValue().getStringValue().equals("SERVER_ERROR")));
                assertTrue(span.span().getAttributesList().stream().anyMatch(a ->
                        a.getKey().equals("exception") && a.getValue().getStringValue().equals("none")));
                mark("O02");
                isolation(app); mark("O03");
                int logStart = app.text().length();
                int before = sink.requests.get();
                sink.mode.set(1);
                for (int i = 0; i < 8; i++) { request(app, false, true, 200, true); }
                Thread.sleep(5000);
                assertTrue(sink.requests.get() > before);
                await(() -> exportFailure(app, logStart, false), 10, "503 export failure log");
                mark("O05");
                int delayStart = app.text().length();
                int delayBefore = sink.requests.get();
                sink.mode.set(2);
                for (int i = 0; i < 8; i++) { request(app, false, true, 200, true); }
                await(() -> sink.requests.get() > delayBefore, 5, "delayed export attempt");
                await(() -> exportFailure(app, delayStart, true), 10, "export timeout log");
                mark("O06");
                sink.mode.set(0);
                verifyNormal(app); mark("O07");
            }
            try (D1Process app = new D1Process(Path.of(java25, "bin/java"), jar, evidence,
                    "vt", sink.port(), true, true, owner)) {
                virtual = true;
                assertTrue(app.ready.getProperty("java").startsWith("25."));
                verifyNormal(app); mark("VT-O01");
                isolation(app); mark("VT-O03");
            }
            sink.healthy();
            complete = true;
        } finally {
            try { if (sink != null) { sink.close(); } }
            finally {
                client.close();
                Files.writeString(evidence.resolve("run-state.txt"), "completed=" + complete + "\nhttpRequests="
                        + requests.get() + "\notlpRequests=" + (sink == null ? 0 : sink.requests.get()) + "\n");
            }
        }
        assertEquals(initialHash, sha(jar));
        assertEquals(identity.path("jarSha256").asText(), sha(jar));
        try (var paths = Files.walk(evidence)) {
            long size = 0;
            for (Path p : paths.filter(Files::isRegularFile).toList()) {
                byte[] data = Files.readAllBytes(p); size += data.length;
                String text = new String(data, java.nio.charset.StandardCharsets.UTF_8);
                assertFalse(text.contains(AUTH_CANARY)); assertFalse(text.contains(BODY_CANARY));
            }
            assertTrue(size < 33_554_432, "Evidence cap exceeded");
        }
        for (String phase : List.of("disabled", "enabled", "vt")) {
            assertTrue(Files.readString(evidence.resolve(phase + "-cleanup.txt")).contains("alive=false reader=false"));
        }
        assertTrue(Files.exists(evidence.resolve("receiver-cleanup.txt")));
        mark("O08");
        assertEquals(10, checkpoints);
        Files.writeString(evidence.resolve("summary.json"), JSON.writeValueAsString(Map.of(
                "logicalCheckpoints", checkpoints, "httpRequests", requests.get(),
                "otlpRequests", sink.requests.get(), "decodedSpans", sink.spans.size(),
                "jarSha256", initialHash, "ownedResourcesCleaned", true)));
    }

    private void verifyNormal(D1Process app) throws Exception {
        var observed = request(app, false, true, 200, true);
        var actual = awaitSpan(observed);
        assertEquals("koiki-d1-consumer", actual.service());
        assertEquals(Span.SpanKind.SPAN_KIND_SERVER, actual.span().getKind());
        assertTrue(actual.span().getEndTimeUnixNano() >= actual.span().getStartTimeUnixNano());
    }
    private void isolation(D1Process app) throws Exception {
        request(app, false, true, 200, true);
        request(app, false, true, 200, true);
        var pool = Executors.newFixedThreadPool(4);
        try {
            List<Callable<JsonNode>> tasks = java.util.stream.IntStream.range(0, 4)
                    .mapToObj(i -> (Callable<JsonNode>) () -> request(app, false, true, 200, true)).toList();
            for (var future : pool.invokeAll(tasks, 10, TimeUnit.SECONDS)) { awaitSpan(future.get()); }
        } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(3, TimeUnit.SECONDS)); }
        JsonNode fresh = request(app, false, false, 200, true);
        assertTrue(fresh.path("requestId").asText().matches("[0-9a-f-]{36}"));
        awaitSpan(fresh);
    }
    private JsonNode request(D1Process app, boolean fail, boolean supplied, int status, boolean logging) throws Exception {
        assertTrue(requests.incrementAndGet() <= 100);
        int id = markers.incrementAndGet();
        String marker = "d1-" + id;
        String trace = "%032x".formatted(id + 1000);
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port()
                + (fail ? "/d1/fail" : "/d1/ok")))
                .timeout(Duration.ofSeconds(3)).header("X-D1-Marker", marker)
                .header("Authorization", AUTH_CANARY).POST(HttpRequest.BodyPublishers.ofString(BODY_CANARY));
        if (supplied) {
            builder.header("X-Request-ID", marker).header("traceparent", "00-" + trace + "-%016x".formatted(id + 2000) + "-01");
        }
        var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(status, response.statusCode());
        JsonNode data = JSON.readTree(response.body());
        assertEquals(marker, data.path("marker").asText());
        assertTrue(data.path("traceId").asText().matches("[0-9a-f]{32}"));
        assertTrue(data.path("spanId").asText().matches("[0-9a-f]{16}"));
        if (supplied) { assertEquals(trace, data.path("traceId").asText()); assertEquals(marker, data.path("requestId").asText()); }
        else { assertNotEquals(trace, data.path("traceId").asText()); }
        assertEquals(Boolean.toString(virtual), data.path("virtual").asText());
        assertEquals(data.path("traceId").asText(), data.path("mdcTraceId").asText());
        assertEquals(data.path("spanId").asText(), data.path("mdcSpanId").asText());
        assertEquals(data.path("requestId").asText(), response.headers().firstValue("X-Request-ID").orElseThrow());
        Files.writeString(evidence.resolve(marker + "-response.json"), data.toString());
        if (logging) { await(() -> logMatches(app, data), 3, "structured log correlation"); }
        return data;
    }
    private boolean logMatches(D1Process app, JsonNode data) {
        try {
            for (String line : app.text().split("\\R")) {
                if (!line.contains("D1_HANDLER") || !line.contains("marker=" + data.path("marker").asText() + ",")
                        && !line.contains("marker=" + data.path("marker").asText() + "}")) { continue; }
                JsonNode logged = JSON.readTree(line);
                return logged.path("traceId").asText().equals(data.path("traceId").asText())
                        && logged.path("spanId").asText().equals(data.path("spanId").asText())
                        && logged.path("requestId").asText().equals(data.path("requestId").asText());
            }
            return false;
        } catch (Exception ex) { throw new AssertionError("Cannot inspect structured fixture log", ex); }
    }
    private OtlpReceiver.Received awaitSpan(JsonNode data) throws Exception {
        await(() -> sink.spans.stream().anyMatch(s -> matches(s, data)), 10, "decoded successful OTLP span");
        return sink.spans.stream().filter(s -> matches(s, data)).findFirst().orElseThrow();
    }
    private boolean matches(OtlpReceiver.Received s, JsonNode data) {
        return s.mode() == 0 && HexFormat.of().formatHex(s.span().getTraceId().toByteArray()).equals(data.path("traceId").asText())
                && HexFormat.of().formatHex(s.span().getSpanId().toByteArray()).equals(data.path("spanId").asText());
    }
    private boolean exportFailure(D1Process app, int offset, boolean timeout) {
        try {
            String t = app.text().substring(offset).toLowerCase(java.util.Locale.ROOT);
            return (t.contains("failed to export") || t.contains("exporting spans failed"))
                    && (!timeout || t.contains("timeout") || t.contains("timed out"));
        } catch (Exception ex) { throw new AssertionError("Cannot inspect export failure", ex); }
    }
    private void await(BooleanSupplier check, int seconds, String label) throws Exception {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        while (!check.getAsBoolean()) {
            sink.healthy();
            if (System.nanoTime() >= until) { fail("Deadline reached: " + label); }
            Thread.sleep(50);
        }
        sink.healthy();
    }
    private void mark(String id) throws Exception {
        checkpoints++;
        Files.writeString(evidence.resolve("checkpoints.txt"), id + " PASS\n", java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
    }
    private static String sha(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}
