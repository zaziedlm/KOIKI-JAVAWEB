package org.koikifw.reference.externalcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.koikifw.reference.externalcheck.adapter.outbound.external.HttpExternalCheckAdapter;
import org.koikifw.reference.externalcheck.application.ExternalCheckFailure;
import org.koikifw.reference.externalcheck.application.ExternalCheckFailure.Kind;
import org.koikifw.reference.externalcheck.application.ExternalCheckService;
import org.koikifw.reference.externalcheck.application.dto.ExternalCheckData.Query;
import org.koikifw.reference.externalcheck.application.port.outbound.ExternalCheckPort;

@Timeout(5)
class ExternalCheckHttpIntegrationTest {
    static final Query QUERY = new Query("USD", "JPY", LocalDate.of(2026, 10, 22));

    @Test void h01ReturnsReferenceValueThroughTheRealProxyAndTransport() throws Exception {
        try (var fixture = new ExternalRateFixture(); var context = fixture.context()) {
            assertThat(context.getBean(ExternalCheckService.class).lookup(QUERY).rate()).isEqualTo("150.12345678");
            assertThat(fixture.requests.get()).isEqualTo(1);
        }
    }

    @Test void h02RetriesExplicit503AtMostTwice() throws Exception {
        try (var fixture = new ExternalRateFixture()) {
            fixture.mode = ExternalRateFixture.Mode.TRANSIENT;
            try (var context = fixture.context()) {
                assertThat(context.getBean(ExternalCheckPort.class).lookup(UUID.randomUUID(), QUERY).rate())
                        .isEqualTo("150.12345678");
                assertThat(fixture.requests.get()).isEqualTo(3);
            }
        }
    }

    @Test void h03DoesNotRetry400() throws Exception { rejected(ExternalRateFixture.Mode.BAD_REQUEST, Kind.PROVIDER_REJECTED); }
    @Test void h04DoesNotRetry404() throws Exception { rejected(ExternalRateFixture.Mode.MISSING, Kind.PROVIDER_REJECTED); }

    @Test void h05RejectsInvalidAndOversizeBodiesWithoutRetry() throws Exception {
        rejected(ExternalRateFixture.Mode.INVALID, Kind.INVALID_PROVIDER_RESPONSE);
        rejected(ExternalRateFixture.Mode.OVERSIZE, Kind.INVALID_PROVIDER_RESPONSE);
    }

    @Test void h06StopsContinuous503WithinFiniteBudget() throws Exception {
        try (var fixture = new ExternalRateFixture()) {
            fixture.mode = ExternalRateFixture.Mode.UNAVAILABLE;
            try (var context = fixture.context()) {
                long start = System.nanoTime();
                assertThatThrownBy(() -> context.getBean(ExternalCheckPort.class).lookup(UUID.randomUUID(), QUERY))
                        .isInstanceOf(ExternalCheckFailure.class);
                assertThat(fixture.requests.get()).isEqualTo(3);
                assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));
            }
        }
    }

    @Test void h07ConnectBlackholeRequiresAnOwnedReproducibleNetworkFixture() {
        Assumptions.assumeTrue(false,
                "UNPROVEN: no approved owned loopback SYN blackhole; connection refusal is not connect timeout");
    }

    @Test void h08HeaderTimeoutDoesNotRetryAndReleasesTransport() throws Exception { timeout(false, Map.of(), 300); }
    @Test void h09BodyTimeoutDoesNotLeaveAnUnboundedRead() throws Exception { timeout(true, Map.of(), 300); }
    @Test void h10ReadTimeoutOverrideTakesEffect() throws Exception {
        timeout(true, Map.of("koiki.api.http.read-timeout", "100ms"), 100);
    }
    @Test void h11DoesNotWaitForOrRetryProvider429() throws Exception { rejected(ExternalRateFixture.Mode.RATE_LIMIT, Kind.PROVIDER_RATE_LIMITED); }

    @Test void h12RejectsTheThirdConcurrentOperationAndRecovers() throws Exception {
        try (var fixture = new ExternalRateFixture()) {
            fixture.mode = ExternalRateFixture.Mode.HOLD;
            fixture.entered = new CountDownLatch(2);
            try (var context = fixture.context(Map.of("koiki.api.http.read-timeout", "1500ms"))) {
                var service = context.getBean(ExternalCheckService.class);
                var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                        new org.koikifw.reference.externalcheck.adapter.inbound.sessionapi.ExternalCheckController(service))
                        .setControllerAdvice(new org.koikifw.reference.externalcheck.adapter.inbound.sessionapi.ExternalCheckApiExceptionHandler())
                        .build();
                var workers = Executors.newFixedThreadPool(2);
                try {
                    var first = workers.submit(() -> service.lookup(QUERY));
                    var second = workers.submit(() -> service.lookup(QUERY));
                    assertThat(fixture.entered.await(1, TimeUnit.SECONDS)).isTrue();
                    mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                    "/spa-api/v1/external-checks/rate?base=USD&quote=JPY&date=2026-10-22"))
                            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isServiceUnavailable())
                            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code")
                                    .value("KOIKI-REF-EXTERNAL-CONCURRENCY_LIMIT"));
                    assertThat(fixture.requests.get()).isEqualTo(2);
                    assertThat(fixture.maximum.get()).isEqualTo(2);
                    fixture.release.countDown();
                    assertThat(first.get(2, TimeUnit.SECONDS).rate()).isEqualTo("150.12345678");
                    assertThat(second.get(2, TimeUnit.SECONDS).rate()).isEqualTo("150.12345678");
                    fixture.mode = ExternalRateFixture.Mode.NORMAL;
                    assertThat(service.lookup(QUERY).rate()).isEqualTo("150.12345678");
                } finally {
                    fixture.release.countDown(); workers.shutdownNow();
                    assertThat(workers.awaitTermination(2, TimeUnit.SECONDS)).isTrue();
                }
            }
        }
    }

    @Test void h13InterruptionStopsRetriesAndReleasesTheWorker() throws Exception {
        try (var fixture = new ExternalRateFixture(); var context = fixture.context()) {
            fixture.mode = ExternalRateFixture.Mode.HEADER_DELAY;
            var finished = new CountDownLatch(1);
            var outcome = new AtomicReference<Kind>();
            var preserved = new java.util.concurrent.atomic.AtomicBoolean();
            Thread worker = new Thread(() -> {
                try { context.getBean(ExternalCheckPort.class).lookup(UUID.randomUUID(), QUERY); }
                catch (ExternalCheckFailure failure) { outcome.set(failure.kind()); preserved.set(Thread.currentThread().isInterrupted()); }
                finally { finished.countDown(); }
            }, "c2-owned-interrupt");
            worker.start();
            try {
                assertThat(fixture.entered.await(1, TimeUnit.SECONDS)).isTrue();
                worker.interrupt();
                assertThat(finished.await(1, TimeUnit.SECONDS)).isTrue();
                assertThat(outcome.get()).isEqualTo(Kind.INTERRUPTED);
                assertThat(preserved.get()).isTrue();
                assertThat(fixture.requests.get()).isEqualTo(1);
            } finally { fixture.release.countDown(); worker.interrupt(); worker.join(2000); assertThat(worker.isAlive()).isFalse(); }
        }
    }

    private static void rejected(ExternalRateFixture.Mode mode, Kind kind) throws Exception {
        try (var fixture = new ExternalRateFixture()) {
            fixture.mode = mode;
            try (var context = fixture.context()) {
                assertThatThrownBy(() -> context.getBean(ExternalCheckPort.class).lookup(UUID.randomUUID(), QUERY))
                        .isInstanceOfSatisfying(ExternalCheckFailure.class, error -> assertThat(error.kind()).isEqualTo(kind));
                assertThat(fixture.requests.get()).isEqualTo(1);
            }
        }
    }

    private static void timeout(boolean body, Map<String, Object> overrides, int expectedMillis) throws Exception {
        try (var fixture = new ExternalRateFixture(); var context = fixture.context(overrides)) {
            fixture.mode = body ? ExternalRateFixture.Mode.BODY_DELAY : ExternalRateFixture.Mode.HEADER_DELAY;
            long start = System.nanoTime();
            assertThatThrownBy(() -> context.getBean(ExternalCheckPort.class).lookup(UUID.randomUUID(), QUERY))
                    .isInstanceOfSatisfying(ExternalCheckFailure.class,
                            error -> assertThat(error.kind()).isEqualTo(Kind.PROVIDER_TIMEOUT));
            long millis = Duration.ofNanos(System.nanoTime() - start).toMillis();
            assertThat(millis).isBetween((long) expectedMillis - 50, 2000L);
            assertThat(fixture.requests.get()).isEqualTo(1);
            fixture.release.countDown();
            fixture.mode = ExternalRateFixture.Mode.NORMAL;
            assertThat(context.getBean(HttpExternalCheckAdapter.class).lookup(UUID.randomUUID(), QUERY).rate())
                    .isEqualTo("150.12345678");
        }
    }
}
