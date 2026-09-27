package org.koikifw.buildsupport.phase4;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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

    @Test
    void resubmissionFilterCanStopAfterTwoCompletionAttempts() throws InterruptedException {
        provider.useIdempotencyKey(true);
        UUID eventId = failedSend();
        assertEquals(1, completionAttempts(eventId));

        AtomicInteger inspected = new AtomicInteger();
        ResubmissionOptions limited = ResubmissionOptions.defaults().withFilter(publication -> {
            if (!(publication.getEvent() instanceof ProbeApproved event)
                    || !event.eventId().equals(eventId)) {
                return false;
            }
            inspected.incrementAndGet();
            return publication.getCompletionAttempts() < 2;
        });

        listener.failOnceAfterSend();
        failed.resubmit(limited);
        await(() -> publicationCount(eventId, "FAILED") == 1 && completionAttempts(eventId) == 2);
        assertEquals(1, inspected.get());

        failed.resubmit(limited);
        assertEquals(2, inspected.get(), "The failed publication was evaluated again");
        assertEquals(1, publicationCount(eventId, "FAILED"));
        assertEquals(2, completionAttempts(eventId));
        assertEquals(1, sendCount(eventId));

        failed.resubmit(ResubmissionOptions.defaults().withFilter(publication ->
                publication.getEvent() instanceof ProbeApproved event
                        && event.eventId().equals(eventId)));
        await(() -> publicationCount(eventId, "COMPLETED") == 1);
        assertEquals(3, completionAttempts(eventId));
        assertEquals(1, sendCount(eventId));
    }

    @Test
    void failedAgeGaugeMeasuresAgeSincePublication() throws InterruptedException {
        provider.useIdempotencyKey(true);
        UUID eventId = failedSend();
        jdbc.update("""
                UPDATE event_publication
                SET publication_date = CURRENT_TIMESTAMP - INTERVAL '5 minutes'
                WHERE serialized_event LIKE ? AND status = 'FAILED'
                """, "%" + eventId + "%");

        double ageSeconds = meters.get("phase4.probe.publication.failed.oldest.seconds").gauge().value();
        assertEquals(true, ageSeconds >= 290, "The gauge uses publication age for the FAILED row");
        double transitionSeconds = meters.get("phase4.probe.publication.failed.transition.oldest.seconds")
                .gauge().value();
        assertEquals(true, transitionSeconds < 60,
                "An old publication is still a recent FAILED transition");

        jdbc.update("""
                UPDATE event_publication
                SET probe_failed_at = CURRENT_TIMESTAMP - INTERVAL '2 minutes'
                WHERE serialized_event LIKE ? AND status = 'FAILED'
                """, "%" + eventId + "%");
        assertEquals(true, meters.get("phase4.probe.publication.failed.transition.oldest.seconds")
                .gauge().value() >= 110);

        failed.resubmit(ResubmissionOptions.defaults().withFilter(publication ->
                publication.getEvent() instanceof ProbeApproved event
                        && event.eventId().equals(eventId)));
        await(() -> publicationCount(eventId, "COMPLETED") == 1);
        assertEquals(0.0, meters.get("phase4.probe.publication.failed.oldest.seconds").gauge().value());
        assertEquals(0.0, meters.get("phase4.probe.publication.failed.transition.oldest.seconds")
                .gauge().value());
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

    private int completionAttempts(UUID eventId) {
        return jdbc.queryForObject("""
                SELECT completion_attempts FROM event_publication
                WHERE serialized_event LIKE ?
                """, Integer.class, "%" + eventId + "%");
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
