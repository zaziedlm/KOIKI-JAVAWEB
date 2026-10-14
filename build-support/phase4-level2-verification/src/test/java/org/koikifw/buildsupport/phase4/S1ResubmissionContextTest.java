package org.koikifw.buildsupport.phase4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.koikifw.buildsupport.phase4.s1fixture.S1ResubmissionFixture;
import org.koikifw.buildsupport.phase4.s1fixture.S1ResubmissionFixture.Event;
import org.koikifw.buildsupport.phase4.s1fixture.S1ResubmissionFixture.Operation;
import org.koikifw.buildsupport.phase4.s1fixture.S1ResubmissionFixture.Probe;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** L3: actual Modulith registry, JDBC and async proxy, with a test-only operation decorator. */
@Timeout(600)
class S1ResubmissionContextTest {
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres@sha256:"
            + "18cfe3ef5e6815560c98237d6216d1e5119702fb0f3894c8785dd58b8bbe5d73")
            .withCommand("postgres", "-c", "max_connections=16")
            .withCreateContainerCmdModifier(command -> command.getHostConfig()
                    .withMemory(1024L * 1024 * 1024).withNanoCPUs(1_000_000_000L));
    private static ConfigurableApplicationContext context;
    private static JdbcTemplate jdbc;
    private static Probe probe;
    private static ThreadPoolTaskExecutor executor;
    private static IncompleteEventPublications incomplete;
    private static FailedEventPublications failed;

    @BeforeAll
    static void start() {
        POSTGRES.start();
        context = new SpringApplicationBuilder(S1ResubmissionFixture.Configuration.class)
                .web(WebApplicationType.NONE).run(
                        "--spring.config.location=optional:classpath:s1-minimum/unused.properties",
                        "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                        "--spring.datasource.username=" + POSTGRES.getUsername(),
                        "--spring.datasource.password=" + POSTGRES.getPassword(),
                        "--spring.datasource.hikari.maximum-pool-size=4",
                        "--spring.flyway.enabled=false", "--spring.jpa.hibernate.ddl-auto=none",
                        "--spring.modulith.events.jdbc.schema-initialization.enabled=true",
                        "--spring.modulith.events.completion-mode=UPDATE",
                        "--spring.modulith.republish-outstanding-events-on-restart=false");
        jdbc = context.getBean(JdbcTemplate.class);
        probe = context.getBean(Probe.class);
        executor = context.getBean(ThreadPoolTaskExecutor.class);
        incomplete = context.getBean(IncompleteEventPublications.class);
        failed = context.getBean(FailedEventPublications.class);
        assertFalse(context.containsBean("probeRunner"));
        assertEquals(1024L * 1024 * 1024, POSTGRES.getContainerInfo().getHostConfig().getMemory());
        assertEquals(1_000_000_000L, POSTGRES.getContainerInfo().getHostConfig().getNanoCPUs());
        System.out.println("S1-L3 container=" + POSTGRES.getContainerId());
    }

    @AfterAll
    static void stop() {
        if (context != null) { context.close(); }
        POSTGRES.stop();
        S1ResubmissionFixture.CONTEXT.remove();
    }

    @BeforeEach
    void reset() throws Exception {
        S1ResubmissionFixture.CONTEXT.remove();
        drain();
        jdbc.update("DELETE FROM event_publication");
        probe.entries.clear();
        probe.sends.clear();
        probe.failAfterSend.set(false);
    }

    @Test
    void l3_01_exactPublicationWithSameEventOtherListenerAndOtherEvent() throws Exception {
        UUID event = seed();
        UUID unrelated = seed();
        Operation operation = operation(event);
        UUID other = id(event, "s1-other");
        int attempts = attempts(operation.publicationId());
        resubmit(operation, exact(operation, attempts));
        await(() -> status(operation.publicationId()).equals("COMPLETED"));
        drain();
        assertEquals("FAILED", status(other));
        assertEquals("FAILED", status(id(unrelated, "s1-target")));
        assertEquals("FAILED", status(id(unrelated, "s1-other")));
        assertEquals(1, probe.sends.size());
        assertEquals(operation, probe.sends.getFirst().operation());
        assertTrue(probe.sends.getFirst().thread().startsWith("s1-real-worker-"));
        assertTrue(probe.entries.stream().anyMatch(e -> e.stage().equals("capture") && operation.equals(e.operation())
                && !e.thread().startsWith("s1-real-worker-")));
        assertClean();
    }

    @Test
    void l3_02_missingContextRefusesSendOnActualExecutor() throws Exception {
        Operation operation = operation(seed());
        int before = attempts(operation.publicationId());
        incomplete.resubmitIncompletePublications(exact(operation, before));
        await(() -> status(operation.publicationId()).equals("FAILED") && attempts(operation.publicationId()) == before + 1);
        drain();
        assertEquals(0, probe.sends.size());
        assertClean();
    }

    @ParameterizedTest
    @ValueSource(strings = {"event", "listener", "publication"})
    void l3_03_wrongContextTargetRefusesSend(String field) throws Exception {
        Operation actual = operation(seed());
        Operation wrong = new Operation(actual.operationId(), field.equals("publication") ? UUID.randomUUID() : actual.publicationId(),
                field.equals("event") ? UUID.randomUUID() : actual.eventId(), field.equals("listener") ? "wrong" : actual.listenerId());
        int before = attempts(actual.publicationId());
        resubmit(wrong, exact(actual, before));
        await(() -> status(actual.publicationId()).equals("FAILED") && attempts(actual.publicationId()) == before + 1);
        drain();
        assertEquals(0, probe.sends.size());
        assertClean();
    }

    @Test
    void l3_04_exceptionAndNextOperationReuseWorkerWithoutLeakingContext() throws Exception {
        Operation first = operation(seed());
        probe.failAfterSend.set(true);
        int before = attempts(first.publicationId());
        resubmit(first, exact(first, before));
        await(() -> status(first.publicationId()).equals("FAILED") && attempts(first.publicationId()) == before + 1);
        drain();
        Operation second = operation(seed());
        resubmit(second, exact(second, attempts(second.publicationId())));
        await(() -> status(second.publicationId()).equals("COMPLETED"));
        drain();
        assertEquals(2, probe.sends.size());
        assertEquals(first, probe.sends.get(0).operation());
        assertEquals(second, probe.sends.get(1).operation());
        assertEquals(probe.sends.get(0).thread(), probe.sends.get(1).thread());
        assertClean();
    }

    @Test
    void l3_05_optionsBatchBeforeFilterCanLeaveTargetUnselected() throws Exception {
        Operation operation = operation(seed());
        jdbc.update("UPDATE event_publication SET publication_date = now() - interval '5 minutes' WHERE id <> ?", operation.publicationId());
        jdbc.update("UPDATE event_publication SET publication_date = now() WHERE id = ?", operation.publicationId());
        int before = attempts(operation.publicationId());
        var inspected = new AtomicInteger();
        S1ResubmissionFixture.CONTEXT.set(operation);
        try {
            failed.resubmit(ResubmissionOptions.defaults().withMinAge(Duration.ZERO).withBatchSize(1).withMaxInFlight(1)
                    .withFilter(p -> { inspected.incrementAndGet(); return exact(operation, before).test(p); }));
        } finally { S1ResubmissionFixture.CONTEXT.remove(); }
        drain();
        assertEquals(1, inspected.get());
        assertEquals(before, attempts(operation.publicationId()));
        assertEquals("FAILED", status(operation.publicationId()));
        assertEquals(0, probe.sends.size());
    }

    @Test
    void l3_06_completedOtherPublicationAndWrongAttemptCannotProveTargetCompletion() throws Exception {
        Operation operation = operation(seed());
        jdbc.update("UPDATE event_publication SET status = 'COMPLETED', completion_date = now() WHERE id = ?", id(operation.eventId(), "s1-other"));
        int before = attempts(operation.publicationId());
        resubmit(operation, exact(operation, before + 1));
        drain();
        assertEquals("FAILED", status(operation.publicationId()));
        assertEquals(before, attempts(operation.publicationId()));
        assertEquals(0, probe.sends.size());
        resubmit(operation, exact(operation, before));
        await(() -> status(operation.publicationId()).equals("COMPLETED"));
        drain();
        assertEquals(1, probe.sends.size());
    }

    private static UUID seed() throws Exception {
        UUID event = UUID.randomUUID();
        context.getBean(TransactionTemplate.class).executeWithoutResult(tx -> context.publishEvent(new Event(event)));
        await(() -> jdbc.queryForObject("SELECT count(*) FROM event_publication WHERE serialized_event::jsonb->>'eventId' = ? AND status = 'FAILED'",
                Integer.class, event.toString()) == 2);
        drain();
        return event;
    }

    private static Operation operation(UUID event) { return new Operation(UUID.randomUUID(), id(event, "s1-target"), event, "s1-target"); }
    private static UUID id(UUID event, String listener) {
        return jdbc.queryForObject("SELECT id FROM event_publication WHERE serialized_event::jsonb->>'eventId' = ? AND listener_id = ?",
                UUID.class, event.toString(), listener);
    }
    private static int attempts(UUID id) { return jdbc.queryForObject("SELECT completion_attempts FROM event_publication WHERE id = ?", Integer.class, id); }
    private static String status(UUID id) { return jdbc.queryForObject("SELECT status FROM event_publication WHERE id = ?", String.class, id); }
    private static Predicate<EventPublication> exact(Operation operation, int attempt) {
        return p -> p.getIdentifier().equals(operation.publicationId()) && p.getStatus() == EventPublication.Status.FAILED
                && p.getCompletionAttempts() == attempt && p.getEvent() instanceof Event e && e.eventId().equals(operation.eventId());
    }
    private static void resubmit(Operation operation, Predicate<EventPublication> predicate) {
        S1ResubmissionFixture.CONTEXT.set(operation);
        try { incomplete.resubmitIncompletePublications(predicate); }
        finally { S1ResubmissionFixture.CONTEXT.remove(); }
    }
    private static void drain() throws Exception { executor.submit(() -> {}).get(10, TimeUnit.SECONDS); }
    private static void assertClean() {
        assertNull(S1ResubmissionFixture.CONTEXT.get());
        assertTrue(probe.entries.stream().filter(e -> e.stage().equals("before-install") || e.stage().equals("restored"))
                .allMatch(e -> e.operation() == null));
        probe.entries.forEach(e -> System.out.println("S1-L3 " + e));
    }
    private static void await(BooleanSupplier condition) {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) { LockSupport.parkNanos(Duration.ofMillis(10).toNanos()); }
        assertTrue(condition.getAsBoolean(), "Expected target publication outcome missing");
    }
}
