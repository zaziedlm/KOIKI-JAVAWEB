package org.koikifw.buildsupport.phase4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Tracks one event through failure and job retry, then checks a second request on the same worker. */
@SpringBootTest(classes = ProbeApplication.class, properties = "probe.correlation.enabled=true")
class CorrelationObservationTest {

    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired ApprovalProbe approvals;
    @Autowired NotificationProbe listener;
    @Autowired RecoveryJobProbe recovery;
    @Autowired CorrelationProbe correlation;
    @Autowired JdbcTemplate jdbc;

    @Test
    void eventIdConnectsInitialListenerAndJobRetryWithoutLeakingRequestContext() throws InterruptedException {
        Logger logger = (Logger) LoggerFactory.getLogger(CorrelationProbe.class);
        var capturedLogs = new ListAppender<ILoggingEvent>();
        capturedLogs.start();
        logger.addAppender(capturedLogs);
        try {
            UUID eventId = UUID.randomUUID();
            listener.failOnceAfterSend();
            MDC.put("requestId", "request-first");
            MDC.put("traceId", "trace-first-probe");
            approvals.approve(eventId);
            MDC.clear();

            await(() -> publicationCount(eventId, "FAILED") == 1);
            UUID publicationId = publicationId(eventId);
            CorrelationProbe.Entry firstListener = listenerEntries(eventId).getFirst();
            assertEquals("request-first", firstListener.context().get("requestId"));
            assertEquals("trace-first-probe", firstListener.context().get("traceId"));
            assertEquals("request-first", entry(eventId, "publish-request").context().get("requestId"));

            UUID jobId = UUID.randomUUID();
            UUID retryId = UUID.randomUUID();
            recovery.retry(eventId, jobId, retryId);
            await(() -> publicationCount(eventId, "COMPLETED") == 1
                    && listenerEntries(eventId).size() == 2);
            assertEquals(publicationId, publicationId(eventId));
            CorrelationProbe.Entry retriedListener = listenerEntries(eventId).get(1);
            assertEquals(jobId.toString(), retriedListener.context().get("jobId"));
            assertEquals(retryId.toString(), retriedListener.context().get("retryId"));
            assertEquals(eventId.toString(), retriedListener.context().get("eventId"));
            assertFalse(retriedListener.context().containsKey("requestId"));
            assertFalse(retriedListener.context().containsKey("traceId"));
            assertEquals(jobId.toString(), entry(eventId, "recovery-job").context().get("jobId"));

            UUID secondEvent = UUID.randomUUID();
            MDC.put("requestId", "request-second");
            MDC.put("traceId", "trace-second-probe");
            approvals.approve(secondEvent);
            MDC.clear();
            await(() -> publicationCount(secondEvent, "COMPLETED") == 1
                    && listenerEntries(secondEvent).size() == 1);
            CorrelationProbe.Entry secondListener = listenerEntries(secondEvent).getFirst();
            assertEquals("request-second", secondListener.context().get("requestId"));
            assertEquals("trace-second-probe", secondListener.context().get("traceId"));
            assertFalse(secondListener.context().containsKey("jobId"));
            assertFalse(secondListener.context().containsKey("retryId"));
            assertFalse(secondListener.context().containsValue("request-first"));

            List<ILoggingEvent> eventLogs = capturedLogs.list.stream()
                    .filter(log -> log.getFormattedMessage().contains(eventId.toString()))
                    .toList();
            assertEquals(4, eventLogs.size());
            assertTrue(eventLogs.stream().anyMatch(log ->
                    "request-first".equals(log.getMDCPropertyMap().get("requestId"))));
            assertTrue(eventLogs.stream().anyMatch(log ->
                    "trace-first-probe".equals(log.getMDCPropertyMap().get("traceId"))));
            assertTrue(eventLogs.stream().anyMatch(log ->
                    jobId.toString().equals(log.getMDCPropertyMap().get("jobId"))));
            assertTrue(capturedLogs.list.stream()
                    .filter(log -> log.getFormattedMessage().contains(secondEvent.toString()))
                    .noneMatch(log -> log.getMDCPropertyMap().containsValue("request-first")
                            || log.getMDCPropertyMap().containsValue("trace-first-probe")
                            || log.getMDCPropertyMap().containsKey("jobId")));
        } finally {
            MDC.clear();
            logger.detachAppender(capturedLogs);
            capturedLogs.stop();
        }
    }

    private CorrelationProbe.Entry entry(UUID eventId, String stage) {
        return correlation.forEvent(eventId).stream()
                .filter(candidate -> candidate.stage().equals(stage))
                .findFirst().orElseThrow();
    }

    private List<CorrelationProbe.Entry> listenerEntries(UUID eventId) {
        return correlation.forEvent(eventId).stream()
                .filter(candidate -> candidate.stage().equals("listener"))
                .toList();
    }

    private UUID publicationId(UUID eventId) {
        return jdbc.queryForObject("""
                SELECT id FROM event_publication WHERE serialized_event LIKE ?
                """, UUID.class, "%" + eventId + "%");
    }

    private int publicationCount(UUID eventId, String status) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM event_publication
                WHERE serialized_event LIKE ? AND status = ?
                """, Integer.class, "%" + eventId + "%", status);
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
        assertTrue(condition.getAsBoolean(), "Expected event correlation state did not appear");
    }
}
