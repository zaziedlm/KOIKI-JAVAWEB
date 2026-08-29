package org.koikifw.performance.runner;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import tools.jackson.databind.json.JsonMapper;

/** External-process workload runner and deterministic raw-result aggregator. */
public final class PerformanceRunner {

    private static final int SCHEMA_VERSION = 1;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private PerformanceRunner() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("command is required");
        }
        Map<String, String> options = options(args);
        switch (args[0]) {
            case "measure" -> measure(options);
            case "startup" -> startup(options);
            case "aggregate" -> aggregate(options);
            default -> throw new IllegalArgumentException("unknown command: " + args[0]);
        }
    }

    private static void measure(Map<String, String> options) throws IOException {
        String runId = required(options, "run-id");
        String variant = required(options, "variant");
        int fork = integer(options, "fork");
        int warmup = integer(options, "warmup");
        int measurement = integer(options, "measurement");
        Duration timeout = Duration.ofMillis(integer(options, "timeout-ms"));
        URI baseUri = URI.create(required(options, "base-url"));
        Path output = Path.of(required(options, "output"));

        HttpClient client = HttpClient.newBuilder().connectTimeout(timeout).build();
        List<Sample> samples = new ArrayList<>(measurement * 3);
        for (Workload workload : Workload.values()) {
            for (int sequence = 0; sequence < warmup; sequence++) {
                invoke(client, baseUri, workload, timeout, variant, fork, sequence, false);
            }
            for (int sequence = 0; sequence < measurement; sequence++) {
                samples.add(invoke(
                        client, baseUri, workload, timeout, variant, fork, sequence, true));
            }
        }
        write(output, new RawResult(SCHEMA_VERSION, runId, samples));
    }

    private static Sample invoke(
            HttpClient client,
            URI baseUri,
            Workload workload,
            Duration timeout,
            String variant,
            int fork,
            int sequence,
            boolean measured) {
        HttpRequest request = request(baseUri, workload, timeout);
        long started = System.nanoTime();
        try {
            HttpResponse<byte[]> response = client.send(
                    request, HttpResponse.BodyHandlers.ofByteArray());
            long duration = System.nanoTime() - started;
            boolean statusMatches = response.statusCode() == workload.expectedStatus;
            boolean success = statusMatches && responseMatches(
                    workload,
                    variant,
                    response.statusCode(),
                    response.body(),
                    response.headers().firstValue("Location").orElse(null));
            return measured ? new Sample(
                    "request", variant, workload.id, fork, sequence, duration,
                    response.statusCode(), response.body().length, success,
                    success ? null : statusMatches ? "UNEXPECTED_RESPONSE" : "UNEXPECTED_STATUS") : null;
        } catch (IOException exception) {
            return failureSample(measured, variant, workload, fork, sequence, started, "IO_FAILURE");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failureSample(measured, variant, workload, fork, sequence, started, "INTERRUPTED");
        }
    }

    private static Sample failureSample(
            boolean measured,
            String variant,
            Workload workload,
            int fork,
            int sequence,
            long started,
            String errorCode) {
        if (!measured) {
            throw new IllegalStateException("warm-up request failed: " + errorCode);
        }
        return new Sample(
                "request", variant, workload.id, fork, sequence,
                System.nanoTime() - started, null, 0, false, errorCode);
    }

    private static HttpRequest request(URI baseUri, Workload workload, Duration timeout) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(baseUri.resolve(workload.path))
                .timeout(timeout)
                .header("Accept", "application/json");
        return switch (workload) {
            case HTTP_SUCCESS -> builder.GET().build();
            case VALIDATION_REJECTION -> builder
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();
            case DB_WRITE -> builder
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"label\":\"performance-" + UUID.randomUUID() + "\"}"))
                    .build();
        };
    }

    static boolean responseMatches(
            Workload workload,
            String variant,
            int status,
            byte[] body,
            String location) {
        if (status != workload.expectedStatus) {
            return false;
        }
        try {
            var json = JSON.readTree(body);
            return switch (workload) {
                case HTTP_SUCCESS -> "ok".equals(json.path("value").asText());
                case VALIDATION_REJECTION -> json.path("status").asInt() == 400
                        && !json.path("title").asText().isBlank()
                        && (!"koiki".equals(variant)
                                || ("KOIKI-VALIDATION-001".equals(json.path("code").asText())
                                        && "Request validation failed."
                                                .equals(json.path("detail").asText())));
                case DB_WRITE -> {
                    String id = json.path("id").asText();
                    UUID.fromString(id);
                    yield ("/performance/1/items/" + id).equals(location);
                }
            };
        } catch (Exception exception) {
            return false;
        }
    }

    private static void startup(Map<String, String> options) throws IOException {
        Sample sample = new Sample(
                "startup",
                required(options, "variant"),
                "startup",
                integer(options, "fork"),
                0,
                Long.parseLong(required(options, "duration-nanos")),
                200,
                0,
                Boolean.parseBoolean(required(options, "success")),
                null);
        write(Path.of(required(options, "output")), new RawResult(
                SCHEMA_VERSION, required(options, "run-id"), List.of(sample)));
    }

    private static void aggregate(Map<String, String> options) throws IOException {
        Path inputDirectory = Path.of(required(options, "input-dir"));
        String runId = required(options, "run-id");
        List<Sample> samples = new ArrayList<>();
        try (var paths = Files.list(inputDirectory)) {
            for (Path path : paths.filter(file -> file.getFileName().toString().endsWith(".fragment.json"))
                    .sorted()
                    .toList()) {
                RawResult fragment = JSON.readValue(path.toFile(), RawResult.class);
                if (!runId.equals(fragment.runId())) {
                    throw new IllegalArgumentException("fragment runId does not match");
                }
                samples.addAll(fragment.samples());
            }
        }
        RawResult raw = new RawResult(SCHEMA_VERSION, runId, samples);
        AggregateResult aggregate = aggregate(raw);
        write(Path.of(required(options, "raw-output")), raw);
        write(Path.of(required(options, "aggregate-output")), aggregate);
    }

    static AggregateResult aggregate(RawResult raw) {
        Map<GroupKey, List<Sample>> groups = raw.samples().stream()
                .collect(Collectors.groupingBy(
                        sample -> new GroupKey(sample.variant(), sample.workload()),
                        LinkedHashMap::new,
                        Collectors.toList()));
        List<WorkloadAggregate> aggregates = groups.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> summarize(entry.getKey(), entry.getValue()))
                .toList();

        Map<String, WorkloadAggregate> bare = aggregates.stream()
                .filter(value -> "bare".equals(value.variant()))
                .collect(Collectors.toMap(WorkloadAggregate::workload, value -> value));
        List<Comparison> comparisons = aggregates.stream()
                .filter(value -> "koiki".equals(value.variant()) && bare.containsKey(value.workload()))
                .map(koiki -> compare(bare.get(koiki.workload()), koiki))
                .toList();
        return new AggregateResult(SCHEMA_VERSION, raw.runId(), aggregates, comparisons);
    }

    private static WorkloadAggregate summarize(GroupKey key, List<Sample> samples) {
        List<Long> durations = samples.stream()
                .map(Sample::durationNanos)
                .sorted()
                .toList();
        long total = durations.stream().mapToLong(Long::longValue).sum();
        long failures = samples.stream().filter(sample -> !sample.success()).count();
        double requestsPerSecond = total == 0 ? 0.0 : samples.size() * 1_000_000_000.0 / total;
        return new WorkloadAggregate(
                key.variant, key.workload, samples.size(), failures,
                percentile(durations, 0.50), percentile(durations, 0.95), total,
                requestsPerSecond);
    }

    static long percentile(List<Long> sortedDurations, double percentile) {
        if (sortedDurations.isEmpty()) {
            return 0L;
        }
        int index = Math.max(0, (int) Math.ceil(percentile * sortedDurations.size()) - 1);
        return sortedDurations.get(index);
    }

    private static Comparison compare(WorkloadAggregate bare, WorkloadAggregate koiki) {
        return new Comparison(
                koiki.workload(),
                koiki.p50Nanos() - bare.p50Nanos(),
                percentage(bare.p50Nanos(), koiki.p50Nanos()),
                koiki.p95Nanos() - bare.p95Nanos(),
                percentage(bare.p95Nanos(), koiki.p95Nanos()));
    }

    private static Double percentage(long bare, long koiki) {
        return bare == 0 ? null : (koiki - bare) * 100.0 / bare;
    }

    private static Map<String, String> options(String[] args) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int index = 1; index < args.length; index++) {
            String argument = args[index];
            if (!argument.startsWith("--") || !argument.contains("=")) {
                throw new IllegalArgumentException("invalid option: " + argument);
            }
            int separator = argument.indexOf('=');
            values.put(argument.substring(2, separator), argument.substring(separator + 1));
        }
        return values;
    }

    private static String required(Map<String, String> options, String name) {
        String value = options.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing option: " + name);
        }
        return value;
    }

    private static int integer(Map<String, String> options, String name) {
        return Integer.parseInt(required(options, name));
    }

    private static void write(Path output, Object value) throws IOException {
        Files.createDirectories(output.toAbsolutePath().getParent());
        JSON.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), value);
    }

    enum Workload {
        HTTP_SUCCESS("http-success", "/performance/1/success", 200),
        VALIDATION_REJECTION("validation-rejection", "/performance/1/validate", 400),
        DB_WRITE("db-write", "/performance/1/items", 201);

        private final String id;
        private final String path;
        private final int expectedStatus;

        Workload(String id, String path, int expectedStatus) {
            this.id = id;
            this.path = path;
            this.expectedStatus = expectedStatus;
        }
    }

    private record GroupKey(String variant, String workload) implements Comparable<GroupKey> {
        @Override
        public int compareTo(GroupKey other) {
            int workloadOrder = workload.compareTo(other.workload);
            return workloadOrder != 0 ? workloadOrder : variant.compareTo(other.variant);
        }
    }

    public record RawResult(int schemaVersion, String runId, List<Sample> samples) { }

    public record Sample(
            String sampleType,
            String variant,
            String workload,
            int fork,
            int sequence,
            long durationNanos,
            Integer httpStatus,
            int responseBytes,
            boolean success,
            String errorCode) { }

    public record AggregateResult(
            int schemaVersion,
            String runId,
            List<WorkloadAggregate> workloads,
            List<Comparison> comparisons) { }

    public record WorkloadAggregate(
            String variant,
            String workload,
            long sampleCount,
            long failureCount,
            long p50Nanos,
            long p95Nanos,
            long totalDurationNanos,
            double requestsPerSecond) { }

    public record Comparison(
            String workload,
            long p50DeltaNanos,
            Double p50DeltaPercent,
            long p95DeltaNanos,
            Double p95DeltaPercent) { }
}
