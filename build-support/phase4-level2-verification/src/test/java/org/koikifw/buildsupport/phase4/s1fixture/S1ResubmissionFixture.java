package org.koikifw.buildsupport.phase4.s1fixture;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Explicitly imported test-only composition; no component scan, MDC authorization or fake trace. */
public final class S1ResubmissionFixture {
    private S1ResubmissionFixture() {}

    public record Event(UUID eventId) {}
    public record Operation(UUID operationId, UUID publicationId, UUID eventId, String listenerId) {}
    public record Entry(String stage, String thread, Operation operation, UUID eventId, String listener) {}
    public static final ThreadLocal<Operation> CONTEXT = new ThreadLocal<>();

    public static class Probe {
        public final List<Entry> entries = new CopyOnWriteArrayList<>();
        public final List<Entry> sends = new CopyOnWriteArrayList<>();
        public final AtomicBoolean failAfterSend = new AtomicBoolean();
        private final JdbcTemplate jdbc;
        public Probe(JdbcTemplate jdbc) { this.jdbc = jdbc; }
        public void record(String stage, UUID event, String listener) {
            entries.add(new Entry(stage, Thread.currentThread().getName(), CONTEXT.get(), event, listener));
        }
        public void handle(Event event, String listener) {
            record("listener", event.eventId(), listener);
            Operation operation = CONTEXT.get();
            if (operation == null || !operation.eventId().equals(event.eventId()) || !operation.listenerId().equals(listener)) {
                throw new IllegalStateException("S1 fixture: missing or mismatched operation");
            }
            Integer matches = jdbc.queryForObject("""
                    SELECT count(*) FROM event_publication
                    WHERE id = ? AND listener_id = ? AND serialized_event::jsonb->>'eventId' = ?
                    """, Integer.class, operation.publicationId(), listener, event.eventId().toString());
            if (matches == null || matches != 1) { throw new IllegalStateException("S1 fixture: publication mismatch"); }
            sends.add(new Entry("send", Thread.currentThread().getName(), operation, event.eventId(), listener));
            if (failAfterSend.getAndSet(false)) { throw new IllegalStateException("S1 fixture: failure after probe send"); }
        }
    }

    public static class Target {
        private final Probe probe;
        public Target(Probe probe) { this.probe = probe; }
        @ApplicationModuleListener(id = "s1-target")
        public void on(Event event) { probe.handle(event, "s1-target"); }
    }

    public static class Other {
        private final Probe probe;
        public Other(Probe probe) { this.probe = probe; }
        @ApplicationModuleListener(id = "s1-other")
        public void on(Event event) { probe.handle(event, "s1-other"); }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EnableAsync
    public static class Configuration {
        @Bean Probe probe(JdbcTemplate jdbc) { return new Probe(jdbc); }
        @Bean Target target(Probe probe) { return new Target(probe); }
        @Bean Other other(Probe probe) { return new Other(probe); }
        @Bean("taskExecutor")
        ThreadPoolTaskExecutor executor(Probe probe) {
            var executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(1);
            executor.setMaxPoolSize(1);
            executor.setQueueCapacity(20);
            executor.setThreadNamePrefix("s1-real-worker-");
            executor.setWaitForTasksToCompleteOnShutdown(true);
            executor.setAwaitTerminationSeconds(10);
            executor.setTaskDecorator(task -> {
                Operation captured = CONTEXT.get();
                probe.record("capture", null, null);
                return () -> {
                    Operation previous = CONTEXT.get();
                    probe.record("before-install", null, null);
                    try {
                        if (captured == null) { CONTEXT.remove(); } else { CONTEXT.set(captured); }
                        task.run();
                    } finally {
                        if (previous == null) { CONTEXT.remove(); } else { CONTEXT.set(previous); }
                        probe.record("restored", null, null);
                    }
                };
            });
            return executor;
        }
    }
}
