package org.koikifw.buildsupport.phase4;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.core.EventPublicationRegistry;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Compares listener recovery with and without an external provider idempotency key. */
@SpringBootTest(classes = ProbeApplication.class)
class PublicationRecoveryTest {

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
    @Autowired ProviderStub provider;
    @Autowired FailedEventPublications failed;
    @Autowired CompletedEventPublications completed;
    @Autowired JdbcTemplate jdbc;
    @Autowired MeterRegistry meters;
    @Autowired EventPublicationRegistry publications;

    @Test
    void retryAfterAcceptedSendRequiresProviderIdempotency() throws InterruptedException {
        UUID protectedEvent = UUID.randomUUID();
        listener.failOnceAfterSend();
        approvals.approve(protectedEvent);
        await(() -> publicationCount(protectedEvent, "FAILED") == 1);
        assertEquals(1, approvalCount(protectedEvent));
        assertEquals(1, sendCount(protectedEvent));
        assertEquals(1.0, meters.get("phase4.probe.publication.failed.count").gauge().value());
        assertEquals(true,
                meters.get("phase4.probe.publication.failed.oldest.seconds").gauge().value() >= 0);

        failed.resubmit(ResubmissionOptions.defaults());
        await(() -> publicationCount(protectedEvent, "COMPLETED") == 1);
        assertEquals(1, sendCount(protectedEvent));
        assertEquals(0.0, meters.get("phase4.probe.publication.failed.count").gauge().value());

        provider.useIdempotencyKey(false);
        UUID unprotectedEvent = UUID.randomUUID();
        listener.failOnceAfterSend();
        approvals.approve(unprotectedEvent);
        await(() -> publicationCount(unprotectedEvent, "FAILED") == 1);
        failed.resubmit(ResubmissionOptions.defaults());
        await(() -> publicationCount(unprotectedEvent, "COMPLETED") == 1);
        assertEquals(2, sendCount(unprotectedEvent));

        Thread.sleep(10);
        completed.deletePublicationsOlderThan(Duration.ofMillis(1));
        await(() -> publicationCount(protectedEvent, "COMPLETED") == 0
                && publicationCount(unprotectedEvent, "COMPLETED") == 0);
    }

    @Test
    void onlyOldProcessingPublicationIsMarkedFailedAndResubmitted() throws InterruptedException {
        provider.useIdempotencyKey(true);
        UUID stale = failedSend();
        UUID recent = failedSend();

        jdbc.update("""
                UPDATE event_publication
                SET status = 'PROCESSING', publication_date = CURRENT_TIMESTAMP - INTERVAL '5 minutes'
                WHERE serialized_event LIKE ? AND status = 'FAILED'
                """, "%" + stale + "%");
        jdbc.update("""
                UPDATE event_publication SET status = 'PROCESSING'
                WHERE serialized_event LIKE ? AND status = 'FAILED'
                """, "%" + recent + "%");

        publications.markStalePublicationsFailed(status ->
                status == EventPublication.Status.PROCESSING ? Duration.ofMinutes(1) : Duration.ZERO);

        assertEquals(1, publicationCount(stale, "FAILED"));
        assertEquals(1, publicationCount(recent, "PROCESSING"));
        failed.resubmit(ResubmissionOptions.defaults().withMaxInFlight(1));
        await(() -> publicationCount(stale, "COMPLETED") == 1);
        assertEquals(1, sendCount(stale));
        assertEquals(1, publicationCount(recent, "PROCESSING"));

        jdbc.update("""
                UPDATE event_publication SET status = 'FAILED'
                WHERE serialized_event LIKE ? AND status = 'PROCESSING'
                """, "%" + recent + "%");
        failed.resubmit(ResubmissionOptions.defaults());
        await(() -> publicationCount(recent, "COMPLETED") == 1);
        assertEquals(1, sendCount(recent));
    }

    private UUID failedSend() throws InterruptedException {
        UUID eventId = UUID.randomUUID();
        listener.failOnceAfterSend();
        approvals.approve(eventId);
        await(() -> publicationCount(eventId, "FAILED") == 1);
        return eventId;
    }

    private int approvalCount(UUID eventId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM probe_approval WHERE event_id = ?", Integer.class, eventId);
    }

    private int sendCount(UUID eventId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM probe_provider_send WHERE event_id = ?", Integer.class, eventId);
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
        assertEquals(true, condition.getAsBoolean(), "Expected publication state did not appear");
    }
}
