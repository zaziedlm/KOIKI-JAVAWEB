package org.koikifw.performance.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.koikifw.performance.runner.PerformanceRunner.RawResult;
import org.koikifw.performance.runner.PerformanceRunner.Sample;
import org.koikifw.performance.runner.PerformanceRunner.Workload;

class PerformanceRunnerTest {

    @Test
    void calculatesNearestRankPercentiles() {
        assertEquals(2L, PerformanceRunner.percentile(List.of(1L, 2L, 3L, 4L), 0.50));
        assertEquals(4L, PerformanceRunner.percentile(List.of(1L, 2L, 3L, 4L), 0.95));
        assertEquals(0L, PerformanceRunner.percentile(List.of(), 0.50));
    }

    @Test
    void aggregatesBothVariantsAndCalculatesReadableDeltas() {
        RawResult raw = new RawResult(1, "run", List.of(
                sample("bare", 100), sample("bare", 200),
                sample("koiki", 150), sample("koiki", 300)));

        var aggregate = PerformanceRunner.aggregate(raw);

        assertEquals(2, aggregate.workloads().size());
        assertEquals(1, aggregate.comparisons().size());
        assertEquals(50L, aggregate.comparisons().getFirst().p50DeltaNanos());
        assertEquals(50.0, aggregate.comparisons().getFirst().p50DeltaPercent());
    }

    @Test
    void omitsPercentageWhenBareValueIsZero() {
        RawResult raw = new RawResult(1, "run", List.of(
                sample("bare", 0), sample("koiki", 10)));

        var comparison = PerformanceRunner.aggregate(raw).comparisons().getFirst();

        assertNull(comparison.p50DeltaPercent());
    }

    @Test
    void validatesHttpAndVariantSpecificRejectionResponses() {
        assertTrue(responseMatches(
                Workload.HTTP_SUCCESS, "bare", 200, "{\"value\":\"ok\"}", null));
        assertFalse(responseMatches(
                Workload.HTTP_SUCCESS, "bare", 200, "{\"value\":\"wrong\"}", null));
        assertTrue(responseMatches(
                Workload.VALIDATION_REJECTION,
                "bare",
                400,
                "{\"title\":\"Bad Request\",\"status\":400}",
                null));
        assertTrue(responseMatches(
                Workload.VALIDATION_REJECTION,
                "koiki",
                400,
                "{\"title\":\"Bad Request\",\"status\":400,"
                        + "\"detail\":\"Request validation failed.\","
                        + "\"code\":\"KOIKI-VALIDATION-001\"}",
                null));
        assertFalse(responseMatches(
                Workload.VALIDATION_REJECTION,
                "koiki",
                400,
                "{\"title\":\"Bad Request\",\"status\":400}",
                null));
    }

    @Test
    void validatesCreatedItemBodyAndVersionedLocation() {
        String id = "11111111-1111-1111-1111-111111111111";
        assertTrue(responseMatches(
                Workload.DB_WRITE,
                "bare",
                201,
                "{\"id\":\"" + id + "\"}",
                "/performance/1/items/" + id));
        assertFalse(responseMatches(
                Workload.DB_WRITE,
                "bare",
                201,
                "{\"id\":\"" + id + "\"}",
                "/performance/items/" + id));
    }

    private static boolean responseMatches(
            Workload workload, String variant, int status, String body, String location) {
        return PerformanceRunner.responseMatches(
                workload, variant, status, body.getBytes(StandardCharsets.UTF_8), location);
    }

    private static Sample sample(String variant, long duration) {
        return new Sample(
                "request", variant, "http-success", 1, 1,
                duration, 200, 2, true, null);
    }
}
