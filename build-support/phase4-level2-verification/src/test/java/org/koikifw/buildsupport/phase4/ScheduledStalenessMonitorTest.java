package org.koikifw.buildsupport.phase4;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Confirms that the scheduled monitor handles both pending and in-progress stale records. */
@SpringBootTest(
        classes = ProbeApplication.class,
        properties = {
            "spring.modulith.events.staleness.published=1m",
            "spring.modulith.events.staleness.processing=1m",
            "spring.modulith.events.staleness.check-interval=200ms"
        })
class ScheduledStalenessMonitorTest {

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
    @Autowired JdbcTemplate jdbc;

    @Test
    void monitorMarksOldPublishedAndProcessingRecordsFailed() throws InterruptedException {
        UUID published = failedSend();
        UUID processing = failedSend();

        makeOld(published, "PUBLISHED");
        makeOld(processing, "PROCESSING");

        await(() -> publicationCount(published, "FAILED") == 1
                && publicationCount(processing, "FAILED") == 1);
        assertEquals(1, sendCount(published));
        assertEquals(1, sendCount(processing));
    }

    private UUID failedSend() throws InterruptedException {
        UUID eventId = UUID.randomUUID();
        listener.failOnceAfterSend();
        approvals.approve(eventId);
        await(() -> publicationCount(eventId, "FAILED") == 1);
        return eventId;
    }

    private void makeOld(UUID eventId, String status) {
        assertEquals(1, jdbc.update("""
                UPDATE event_publication
                SET status = ?, publication_date = CURRENT_TIMESTAMP - INTERVAL '5 minutes'
                WHERE serialized_event LIKE ? AND status = 'FAILED'
                """, status, "%" + eventId + "%"));
    }

    private int publicationCount(UUID eventId, String status) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM event_publication
                WHERE serialized_event LIKE ? AND status = ?
                """, Integer.class, "%" + eventId + "%", status);
    }

    private int sendCount(UUID eventId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM probe_provider_send WHERE event_id = ?", Integer.class, eventId);
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
        assertEquals(true, condition.getAsBoolean(), "Scheduled monitor did not mark stale records");
    }
}
