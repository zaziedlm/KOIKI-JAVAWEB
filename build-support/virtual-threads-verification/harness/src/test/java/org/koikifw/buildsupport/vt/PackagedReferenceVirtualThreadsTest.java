package org.koikifw.buildsupport.vt;

import static org.junit.jupiter.api.Assertions.*;
import static org.koikifw.buildsupport.vt.VtReferenceFixture.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Six sequential invocations; 43 logical checkpoints, not 43 JUnit invocations. */
class PackagedReferenceVirtualThreadsTest {
    private static final AtomicBoolean HALTED = new AtomicBoolean();
    private static final String REQUESTS = "/spa-api/v1/expense-requests";

    static Stream<Arguments> configurations() {
        if (Boolean.getBoolean("koiki.vt.pool-diagnostic.enabled")) return Stream.of(Arguments.of("A", true));
        var labels = List.of("A-native", "A-observed", "B-native", "B-observed", "C-native", "C-observed");
        int first = labels.indexOf(System.getProperty("koiki.vt.start-at", "A-native"));
        assertTrue(first >= 0, "Unknown frozen configuration start point");
        return Stream.of(Arguments.of("A", false), Arguments.of("A", true), Arguments.of("B", false),
                Arguments.of("B", true), Arguments.of("C", false), Arguments.of("C", true)).skip(first);
    }

    @ParameterizedTest(name = "{0} observed={1}")
    @MethodSource("configurations")
    @Timeout(value = 6, unit = TimeUnit.MINUTES)
    void checksPackagedReference(String mode, boolean observed) throws Exception {
        Assumptions.assumeFalse(HALTED.get(), "Prior invocation failed; later configurations remain blocked");
        try (var fixture = new VtReferenceFixture(mode, observed); var a = new VtSessionClient(fixture)) {
            if (Boolean.getBoolean("koiki.vt.pool-diagnostic.enabled")) {
                diagnosePool(fixture); return;
            }
            a.login(EMAIL_A);
            if (!observed) {
                UUID id = create(a); assertDetail(a, id, "DRAFT", "1");
                submit(a, id); assertDetail(a, id, "SUBMITTED", "2");
                var mvc = a.get("/expenses/" + id); assertEquals(200, mvc.statusCode());
                assertTrue(mvc.body().contains("SUBMITTED") && mvc.body().contains("100"));
                assertState(fixture, id, "SUBMITTED", 2, 1);
                checkpoint(fixture, "N01");
            } else runObserved(fixture, a);
        } catch (Exception | AssertionError failure) {
            HALTED.set(true); throw failure;
        }
    }

    /** Read-only startup diagnosis; never counted as one of the 43 acceptance checkpoints. */
    private static void diagnosePool(VtReferenceFixture f) throws Exception {
        assertEquals("A", f.mode); assertTrue(f.observed);
        Thread.sleep(10000);
        var snapshots = new ArrayList<Map<String, Object>>();
        try (var connection = f.connection(); var statement = connection.createStatement()) {
            statement.setQueryTimeout(5);
            try (var rows = statement.executeQuery("""
                    select pid, state, coalesce(wait_event_type,'-'), coalesce(wait_event,'-'),
                           md5(query), coalesce(extract(epoch from (now()-xact_start)),0)::bigint
                    from pg_stat_activity where application_name='d2-app' order by pid
                    """)) {
                while (rows.next()) snapshots.add(Map.of("pid", rows.getInt(1), "state", rows.getString(2),
                        "waitType", rows.getString(3), "waitEvent", rows.getString(4), "queryHash", rows.getString(5),
                        "transactionAgeSeconds", rows.getLong(6)));
            }
        }
        Files.writeString(f.evidence.resolve("pool-diagnostic.json"), JSON.writerWithDefaultPrettyPrinter().writeValueAsString(
                Map.of("diagnosticOnly", true, "observationSeconds", 10, "databaseConnections", snapshots)) + "\n");
        Files.writeString(f.evidence.resolve("diagnostic-only.txt"), "No login, business mutation, fault injection, load or checkpoint acceptance.\n");
        System.out.println("D2_DIAGNOSTIC_CAPTURED startup only; acceptance checkpoints not executed");
    }

    private static void runObserved(VtReferenceFixture f, VtSessionClient a) throws Exception {
        String marker = "d2-apply-" + UUID.randomUUID();
        assertEquals(200, a.get("/spa-api/v1/expense-options", marker).statusCode());
        f.awaitRecords(rows -> rows.stream().anyMatch(r -> r[0].equals("H") && r[1].equals(marker)),
                Duration.ofSeconds(2), "handler observation");
        var config = f.records().stream().filter(r -> r[0].equals("CONFIG")).findFirst().orElseThrow();
        assertEquals(f.feature, Integer.parseInt(config[1])); assertEquals(Boolean.toString(f.virtual), config[3]);
        assertEquals("805306368", config[4]); assertEquals("4", config[5]); assertEquals("1", config[6]); assertEquals("3000", config[7]);
        var handler = f.records().stream().filter(r -> r[0].equals("H") && r[1].equals(marker)).findFirst().orElseThrow();
        assertEquals(USER_A, handler[2]); assertEquals(Boolean.toString(f.virtual), handler[3]);
        for (String path : List.of("/actuator", "/actuator/health")) {
            assertTrue(java.util.Set.of(403, 404).contains(a.get(path).statusCode()), "Management HTTP endpoint must not be published");
        }
        checkpoint(f, "V01");

        UUID submitted = create(a);
        assertEquals("1", JSON.readTree(a.get(REQUESTS).body()).path("totalElements").asText());
        assertDetail(a, submitted, "DRAFT", "1"); assertState(f, submitted, "DRAFT", 1, 0); checkpoint(f, "V02");
        String submitRequestId = submit(a, submitted);
        var mvc = a.get("/expenses/" + submitted); assertEquals(200, mvc.statusCode());
        assertTrue(mvc.body().contains("SUBMITTED") && mvc.body().contains("100"));
        assertState(f, submitted, "SUBMITTED", 2, 1);
        assertEquals(1L, f.scalar("select count(*) from koiki_audit_event where action='SUBMIT_EXPENSE' and actor_id='"
                + USER_A + "' and resource_id='" + submitted + "' and request_id='" + submitRequestId + "'"));
        checkpoint(f, "V03");
        try (var anonymous = new VtSessionClient(f)) {
            String requestId = UUID.randomUUID().toString();
            assertEquals(401, anonymous.get(REQUESTS, requestId).statusCode());
            f.awaitRecords(rows -> rows.stream().anyMatch(r -> r[0].equals("F") && r[4].equals(requestId)
                            && r[2].equals("true") && r[3].equals("401")),
                    Duration.ofSeconds(2), "unauthenticated request completion");
            assertFalse(f.records().stream().anyMatch(r -> r[0].equals("H") && r[1].equals(requestId)));
        }
        checkpoint(f, "V04");

        UUID draft = create(a);
        assertEquals(403, a.post(REQUESTS + "/" + draft + "/submit", "{\"expectedVersion\":\"1\"}", null).statusCode());
        assertEquals(403, a.post(REQUESTS + "/" + draft + "/submit", "{\"expectedVersion\":\"1\"}", "incorrect").statusCode());
        assertState(f, draft, "DRAFT", 1, 0); checkpoint(f, "V05");
        try (var b = new VtSessionClient(f)) {
            b.login(EMAIL_B);
            assertEquals(404, b.get(REQUESTS + "/" + draft).statusCode());
            assertEquals(404, b.post(REQUESTS + "/" + draft + "/submit", "{\"expectedVersion\":\"1\"}", b.csrf()).statusCode());
            assertState(f, draft, "DRAFT", 1, 0); checkpoint(f, "V06");
            assertEquals(409, a.post(REQUESTS + "/" + draft + "/submit", "{\"expectedVersion\":\"0\"}", a.csrf()).statusCode());
            assertState(f, draft, "DRAFT", 1, 0); checkpoint(f, "V07");
            try (var executor = Executors.newFixedThreadPool(2)) {
                List<Callable<Void>> calls = List.of(() -> { separatedReads(f, a, USER_A, draft, 200); return null; },
                        () -> { separatedReads(f, b, USER_B, draft, 404); return null; });
                for (var result : executor.invokeAll(calls, 15, TimeUnit.SECONDS)) result.get(1, TimeUnit.SECONDS);
            }
            checkpoint(f, "V08");
        }
        for (String input : new String[]{"d2-explicit-" + UUID.randomUUID(), null, "invalid request id"}) {
            var response = a.get(REQUESTS + "/" + draft, input); assertEquals(200, response.statusCode());
            String actual = response.headers().firstValue("X-Request-ID").orElseThrow();
            if (input != null && !input.contains(" ")) assertEquals(input, actual);
            else { assertNotEquals(input, actual); UUID.fromString(actual); }
            f.awaitRecords(rows -> rows.stream().anyMatch(r -> r[0].equals("H") && r[1].equals(actual))
                            && rows.stream().anyMatch(r -> r[0].equals("F") && r[1].equals(actual) && r[2].equals("true")),
                    Duration.ofSeconds(2), "requestId propagation and cleanup");
        }
        checkpoint(f, "V09");

        List<VtSessionClient> workers = new ArrayList<>();
        try {
            for (int i = 0; i < 8; i++) { var worker = new VtSessionClient(f); workers.add(worker); worker.login(EMAIL_A); }
            exercisePool(f, workers, draft, false); assertState(f, draft, "DRAFT", 1, 0); checkpoint(f, "V10");
            exercisePool(f, workers, draft, true); assertState(f, draft, "DRAFT", 1, 0); checkpoint(f, "V11");
            assertDetail(a, draft, "DRAFT", "1");
            awaitIdle(f);
            String oldSession = a.session();
            assertEquals(204, a.post("/spa-api/logout", "{}", a.csrf()).statusCode());
            try (var old = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()) {
                var response = old.send(HttpRequest.newBuilder(URI.create(f.base + REQUESTS)).timeout(Duration.ofSeconds(10))
                        .header("Cookie", "SESSION=" + oldSession).GET().build(), HttpResponse.BodyHandlers.discarding());
                assertEquals(401, response.statusCode());
            }
            checkpoint(f, "V12");
            if (f.feature == 25) {
                for (int parallel : List.of(1, 8)) {
                    assertState(f, draft, "DRAFT", 1, 0);
                    load(f, workers.subList(0, parallel), draft, parallel, true);
                    load(f, workers.subList(0, parallel), draft, parallel, false);
                    awaitIdle(f); assertState(f, draft, "DRAFT", 1, 0);
                    checkpoint(f, parallel == 1 ? "L01" : "L02");
                }
            }
        } finally { for (var worker : workers) worker.close(); }
        f.awaitRecords(rows -> rows.stream().filter(r -> r[0].equals("H")).count()
                        == rows.stream().filter(r -> r[0].equals("C")).count(),
                Duration.ofSeconds(2), "all observed handlers completed");
        var rows = f.records();
        assertTrue(rows.stream().filter(r -> r[0].equals("F")).allMatch(r -> r[2].equals("true")), "Context leaked after a request");
        assertTrue(rows.stream().filter(r -> r[0].equals("H")).allMatch(r -> r[2].equals(USER_A) || r[2].equals(USER_B)));
        assertTrue(rows.stream().filter(r -> r[0].equals("H")).allMatch(r -> r[3].equals(Boolean.toString(f.virtual))));
        assertTrue(rows.stream().filter(r -> r[0].equals("M")).allMatch(r -> Integer.parseInt(r[2]) <= 4 && Integer.parseInt(r[4]) <= 4));
    }

    private static UUID create(VtSessionClient a) throws Exception {
        String body = """
                {"departmentId":"%s","claimedAmount":"100","lines":[{"expenseCategoryId":"%s",
                "usageDate":"2026-09-12","description":"D2 train","purpose":"D2 compatibility","amount":"100"}]}
                """.formatted(DEPARTMENT, CATEGORY);
        var response = a.post(REQUESTS, body, a.csrf()); assertEquals(201, response.statusCode());
        return UUID.fromString(JSON.readTree(response.body()).path("expenseRequestId").asText());
    }

    private static void assertDetail(VtSessionClient a, UUID id, String status, String version) throws Exception {
        var response = a.get(REQUESTS + "/" + id); assertEquals(200, response.statusCode());
        var body = JSON.readTree(response.body());
        assertEquals(id.toString(), body.path("expenseRequestId").asText());
        assertEquals(status, body.path("status").asText()); assertEquals(version, body.path("version").asText());
        assertEquals("100", body.path("claimedAmount").asText());
    }

    private static String submit(VtSessionClient a, UUID id) throws Exception {
        var response = a.post(REQUESTS + "/" + id + "/submit", "{\"expectedVersion\":\"1\"}", a.csrf());
        assertEquals(204, response.statusCode());
        String requestId = response.request().headers().firstValue("X-Request-ID").orElseThrow();
        UUID.fromString(requestId);
        if (a.fixture.observed) {
            assertEquals(requestId, response.headers().firstValue("X-Request-ID").orElseThrow(),
                    "Observed configuration requires the real correlation filter");
        }
        return requestId;
    }

    private static void assertState(VtReferenceFixture f, UUID id, String state, int version, int audits) throws Exception {
        assertEquals(1L, f.scalar("select count(*) from kkref_expense_request where expense_request_id='" + id
                + "' and status='" + state + "' and version=" + version + " and claimed_amount=100"));
        assertEquals(audits, f.scalar("select count(*) from koiki_audit_event where event_type='EXPENSE_WORKFLOW' and resource_id='" + id + "'"));
    }

    private static void separatedReads(VtReferenceFixture f, VtSessionClient client, String actor, UUID draft, int expected) throws Exception {
        for (int i = 0; i < 5; i++) {
            String marker = UUID.randomUUID().toString();
            assertEquals(expected, client.get(REQUESTS + "/" + draft, marker).statusCode());
            var list = client.get(REQUESTS); assertEquals(200, list.statusCode());
            assertEquals(actor.equals(USER_A) ? "2" : "0", JSON.readTree(list.body()).path("totalElements").asText());
            f.awaitRecords(rows -> rows.stream().anyMatch(r -> r[0].equals("H") && r[1].equals(marker) && r[2].equals(actor)),
                    Duration.ofSeconds(2), "parallel user isolation");
        }
    }

    private static void exercisePool(VtReferenceFixture f, List<VtSessionClient> workers, UUID draft, boolean timeout) throws Exception {
        try (var lock = f.connection(); var executor = Executors.newFixedThreadPool(8)) {
            lock.setAutoCommit(false);
            try (var statement = lock.createStatement()) {
                statement.setQueryTimeout(2);
                statement.execute("set local idle_in_transaction_session_timeout='10s'");
                statement.execute("lock table kkref_expense_request in access exclusive mode");
            }
            long startedNanos = System.nanoTime(); long startedMillis = System.currentTimeMillis();
            List<Future<HttpResponse<String>>> results = new ArrayList<>();
            try {
                for (var client : workers) results.add(executor.submit(() -> client.get(REQUESTS + "/" + draft)));
                Duration observationLimit = Duration.ofMillis(1500);
                f.awaitRecords(rows -> rows.stream().anyMatch(r -> r[0].equals("M") && Long.parseLong(r[1]) >= startedMillis
                                && Integer.parseInt(r[2]) == 4 && Integer.parseInt(r[5]) > 0), observationLimit, "pool waiting under owned DB lock");
                assertTrue(f.scalar("select count(*) from pg_locks l join pg_stat_activity a using(pid) where a.application_name='d2-app'"
                        + " and l.relation='kkref_expense_request'::regclass and not l.granted") > 0, "Relation lock wait not established");
                if (timeout) Thread.sleep(4000);
                assertTrue(System.nanoTime() - startedNanos < Duration.ofSeconds(timeout ? 6 : 2).toNanos(), "Lock duration exceeded plan");
            } finally { lock.rollback(); }
            int failures = 0;
            int earlyRefusals = 0;
            for (var result : results) {
                var response = result.get(10, TimeUnit.SECONDS);
                if (response.statusCode() >= 500 && response.statusCode() < 600) {
                    failures++;
                    assertFalse(response.body().contains("jdbc:") || response.body().contains("SQLTransientConnectionException")
                            || response.body().contains(f.password), "Dependency failure leaked internal data");
                } else if (timeout && response.statusCode() == 403) {
                    // A JDBC Session read can fail before MVC; existing ERROR dispatch remains default-deny.
                    // This is not counted as the required 5xx timeout evidence.
                    String marker = response.request().headers().firstValue("X-Request-ID").orElseThrow();
                    f.awaitRecords(rows -> rows.stream().anyMatch(r -> r[0].equals("F") && r[4].equals(marker)
                                    && r[2].equals("true")), Duration.ofSeconds(2), "pre-MVC timeout completion");
                    assertFalse(f.records().stream().anyMatch(r -> r[0].equals("H") && r[1].equals(marker)),
                            "Only pre-MVC refusals are classified separately");
                    assertFalse(response.body().contains("jdbc:") || response.body().contains("SQLTransientConnectionException")
                            || response.body().contains(f.password), "Refusal leaked internal data");
                    earlyRefusals++;
                } else assertEquals(200, response.statusCode());
            }
            if (timeout) {
                assertTrue(failures > 0, "No finite HTTP pool timeout failure was observed");
                String log = Files.readString(f.processLog);
                assertTrue(log.contains("Connection is not available") && log.contains("3000"), "Pool timeout cause evidence missing");
            } else assertEquals(0, failures);
            assertTrue(f.scalar("select count(*) from pg_stat_activity where datname=current_database() and backend_type='client backend'") <= 8);
            Files.writeString(f.evidence.resolve(timeout ? "pool-timeout.txt" : "pool-wait.txt"),
                    "requests=8\nhttp5xx=" + failures + "\npreMvc403=" + earlyRefusals
                            + "\nlockReleased=true\nrelationWaitObserved=true\n");
        }
        awaitIdle(f);
    }

    private static void awaitIdle(VtReferenceFixture f) throws Exception {
        long since = System.currentTimeMillis();
        f.awaitRecords(rows -> rows.stream().anyMatch(r -> r[0].equals("M") && Long.parseLong(r[1]) >= since
                        && r[2].equals("0") && r[5].equals("0")), Duration.ofSeconds(10), "pool returned to idle");
    }

    private static void load(VtReferenceFixture f, List<VtSessionClient> workers, UUID draft, int parallel, boolean warmup) throws Exception {
        int limit = warmup ? 500 : 2000;
        long duration = Duration.ofSeconds(warmup ? 30 : 60).toNanos();
        long started = System.nanoTime(); long startedMillis = System.currentTimeMillis();
        AtomicInteger slots = new AtomicInteger();
        List<Long> latency = Collections.synchronizedList(new ArrayList<>());
        try (var executor = Executors.newFixedThreadPool(parallel)) {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (var client : workers) tasks.add(() -> {
                while (System.nanoTime() - started < duration && slots.getAndIncrement() < limit) {
                    long before = System.nanoTime();
                    assertDetail(client, draft, "DRAFT", "1");
                    latency.add(System.nanoTime() - before);
                }
                return null;
            });
            for (var future : executor.invokeAll(tasks, warmup ? 41 : 71, TimeUnit.SECONDS)) future.get(1, TimeUnit.SECONDS);
        }
        long elapsed = System.nanoTime() - started;
        assertFalse(latency.isEmpty()); assertTrue(latency.size() <= limit);
        latency.sort(Long::compare);
        var result = Map.of("mode", f.mode, "parallel", parallel, "warmup", warmup, "startedAtMillis", startedMillis,
                "durationSeconds", elapsed / 1_000_000_000d, "completed", latency.size(), "requestsPerSecond", latency.size() * 1_000_000_000d / elapsed,
                "p50Millis", percentile(latency, .50), "p95Millis", percentile(latency, .95), "p99Millis", percentile(latency, .99));
        Files.writeString(f.evidence.resolve("load-" + parallel + (warmup ? "-warmup" : "-measurement") + ".json"),
                JSON.writerWithDefaultPrettyPrinter().writeValueAsString(result) + "\n");
        Files.writeString(f.evidence.resolve("latency-" + parallel + (warmup ? "-warmup" : "-measurement") + ".ns"),
                latency.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining("\n")) + "\n");
    }

    private static double percentile(List<Long> values, double fraction) {
        return values.get(Math.min(values.size() - 1, (int) Math.ceil(values.size() * fraction) - 1)) / 1_000_000d;
    }

    private static void checkpoint(VtReferenceFixture fixture, String id) throws Exception {
        Files.writeString(fixture.evidence.resolve("checkpoints.txt"), id + " PASS\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        System.out.println("D2_CHECKPOINT " + fixture.mode + " observed=" + fixture.observed + " " + id + " PASS");
    }
}
