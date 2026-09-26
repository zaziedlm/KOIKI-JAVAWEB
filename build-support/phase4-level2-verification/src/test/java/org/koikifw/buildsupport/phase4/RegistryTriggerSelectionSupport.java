package org.koikifw.buildsupport.phase4;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Shared database observation for the two registry-trigger configurations. */
abstract class RegistryTriggerSelectionSupport {

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
    @Autowired DirectTransactionalListenerProbe direct;
    @Autowired JdbcTemplate jdbc;

    void assertPublicationCount(int expected) throws InterruptedException {
        UUID eventId = UUID.randomUUID();
        approvals.approve(eventId);
        await(() -> completedCount(eventId) == expected);
        assertEquals(1, direct.invocations(), "Direct listener must still receive the event");
        assertEquals(expected, totalCount(eventId));
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM probe_provider_send WHERE event_id = ?",
                Integer.class, eventId));
    }

    private int completedCount(UUID eventId) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM event_publication
                WHERE serialized_event LIKE ? AND status = 'COMPLETED'
                """, Integer.class, "%" + eventId + "%");
    }

    private int totalCount(UUID eventId) {
        return jdbc.queryForObject("""
                SELECT count(*) FROM event_publication WHERE serialized_event LIKE ?
                """, Integer.class, "%" + eventId + "%");
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
        assertEquals(true, condition.getAsBoolean(), "Expected completed publications did not appear");
    }
}
