package org.koikifw.buildsupport.phase4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** L2 Tooling policy prototype, never formal Reference code or a real crash harness. */
@Timeout(600)
class S1PermitConcurrencyTest {
    private static final String PERMIT = "00000000-0000-0000-0000-000000000001";
    private static final String IMAGE = "postgres@sha256:"
            + "18cfe3ef5e6815560c98237d6216d1e5119702fb0f3894c8785dd58b8bbe5d73";
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(IMAGE)
            .withCommand("postgres", "-c", "max_connections=16")
            .withCreateContainerCmdModifier(command -> command.getHostConfig()
                    .withMemory(1024L * 1024 * 1024).withNanoCPUs(1_000_000_000L));
    private final AtomicInteger sends = new AtomicInteger();

    enum Outcome { SENT, ALREADY_CONSUMED, CLOSED, HELD, COMMITTED_WITHOUT_SEND, UNKNOWN, ISSUED, DUPLICATE }

    @BeforeAll
    static void startDatabase() throws SQLException {
        POSTGRES.start();
        try (var connection = admin()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("s1-minimum/permit-storage.sql"));
            System.out.println("S1-L2 database=" + scalar(connection, "SHOW server_version")
                    + " max_connections=" + scalar(connection, "SHOW max_connections")
                    + " container=" + POSTGRES.getContainerId());
        }
        var host = POSTGRES.getContainerInfo().getHostConfig();
        assertEquals(1024L * 1024 * 1024, host.getMemory());
        assertEquals(1_000_000_000L, host.getNanoCPUs());
    }

    @AfterAll
    static void stopDatabase() { POSTGRES.stop(); }

    @BeforeEach
    void seedPermit() throws SQLException {
        sends.set(0);
        try (var connection = admin()) {
            execute(connection, "TRUNCATE s1.audit_probe, s1.consumption, s1.permit");
        }
        assertEquals(Outcome.ISSUED, issue(PERMIT));
    }

    @Test
    void l2_01_samePermitConcurrentConsumptionSendsOnlyOnce() throws Exception {
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return consume(() -> {}, new AtomicInteger(), false, false);
            });
            var second = executor.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return consume(() -> {}, new AtomicInteger(), false, false);
            });
            var outcomes = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertTrue(outcomes.contains(Outcome.SENT));
            assertTrue(outcomes.contains(Outcome.ALREADY_CONSUMED));
            System.out.println("S1-L2 same-permit outcomes=" + outcomes);
        }
        assertState(1, false, 1);
        assertEquals(1, sends.get());
    }

    @Test
    void l2_02_twoPermitIdsForSameTargetCannotBothBeIssued() throws Exception {
        try (var connection = admin()) { execute(connection, "TRUNCATE s1.audit_probe, s1.consumption, s1.permit"); }
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { barrier.await(10, TimeUnit.SECONDS); return issue(PERMIT); });
            var second = executor.submit(() -> { barrier.await(10, TimeUnit.SECONDS); return issue(UUID.randomUUID().toString()); });
            var outcomes = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertTrue(outcomes.contains(Outcome.ISSUED));
            assertTrue(outcomes.contains(Outcome.DUPLICATE));
            System.out.println("S1-L2 same-target outcomes=" + outcomes);
        }
        try (var connection = admin()) { assertEquals("1", scalar(connection, "SELECT count(*) FROM s1.permit")); }
        assertEquals(0, sends.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONFIRM", "CANCEL"})
    void l2_03_consumptionFirstMakesConcurrentClosureHold(String action) throws Exception {
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var waiterPid = new AtomicInteger();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> consume(() -> hold(locked, release), new AtomicInteger(), false, false));
            try {
                assertTrue(locked.await(10, TimeUnit.SECONDS));
                var second = executor.submit(() -> close(action, () -> {}, waiterPid, false));
                awaitBlocked(waiterPid);
                release.countDown();
                assertEquals(Outcome.SENT, first.get(10, TimeUnit.SECONDS));
                assertEquals(Outcome.HELD, second.get(10, TimeUnit.SECONDS));
            } finally { release.countDown(); }
        }
        assertState(1, false, 1);
        assertEquals(1, sends.get());
        assertEquals(Outcome.DUPLICATE, issue(UUID.randomUUID().toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONFIRM", "CANCEL"})
    void l2_04_closureFirstMakesConcurrentConsumptionRefuse(String action) throws Exception {
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var waiterPid = new AtomicInteger();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> close(action, () -> hold(locked, release), new AtomicInteger(), false));
            try {
                assertTrue(locked.await(10, TimeUnit.SECONDS));
                var second = executor.submit(() -> consume(() -> {}, waiterPid, false, false));
                awaitBlocked(waiterPid);
                release.countDown();
                assertEquals(Outcome.CLOSED, first.get(10, TimeUnit.SECONDS));
                assertEquals(Outcome.CLOSED, second.get(10, TimeUnit.SECONDS));
            } finally { release.countDown(); }
        }
        assertState(0, true, 1);
        assertEquals(0, sends.get());
    }

    @Test
    void l2_05_runnerExitAfterCommitNeverReusesConsumedPermit() throws SQLException {
        assertEquals(Outcome.COMMITTED_WITHOUT_SEND, consume(() -> {}, new AtomicInteger(), true, false));
        assertState(1, false, 1);
        assertEquals(0, sends.get());
        assertEquals(Outcome.ALREADY_CONSUMED, consume(() -> {}, new AtomicInteger(), false, false));
        assertEquals(Outcome.HELD, close("CANCEL", () -> {}, new AtomicInteger(), false));
        assertState(1, false, 1);
        assertEquals(0, sends.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void l2_06_unknownCommitRefusesRestartRegardlessOfActualDbOutcome(boolean actuallyCommitted) throws SQLException {
        // Simulate loss of result knowledge, not a physical transport failure.
        try (var connection = login("s1_worker")) {
            connection.setAutoCommit(false);
            execute(connection, "SELECT permit_id FROM s1.permit FOR UPDATE");
            execute(connection, consumption());
            execute(connection, audit("CONSUMED"));
            if (actuallyCommitted) { connection.commit(); } else { connection.rollback(); }
        }
        assertEquals(Outcome.UNKNOWN, consume(() -> {}, new AtomicInteger(), false, true));
        assertEquals(Outcome.HELD, close("CANCEL", () -> {}, new AtomicInteger(), true));
        assertState(actuallyCommitted ? 1 : 0, false, actuallyCommitted ? 1 : 0);
        assertEquals(Outcome.DUPLICATE, issue(UUID.randomUUID().toString()));
        assertEquals(0, sends.get());
    }

    private Outcome consume(Runnable afterLock, AtomicInteger pid, boolean exitAfterCommit, boolean unknown) throws SQLException {
        if (unknown) { return Outcome.UNKNOWN; }
        try (var connection = login("s1_worker")) {
            connection.setAutoCommit(false);
            pid.set(Integer.parseInt(scalar(connection, "SELECT pg_backend_pid()")));
            execute(connection, "SELECT permit_id FROM s1.permit WHERE permit_id = '" + PERMIT + "' FOR UPDATE");
            afterLock.run();
            if (Boolean.parseBoolean(scalar(connection, "SELECT closed_at IS NOT NULL OR expires_at <= now()"
                    + " FROM s1.permit WHERE permit_id = '" + PERMIT + "'"))) {
                connection.rollback();
                return Outcome.CLOSED;
            }
            if (!scalar(connection, "SELECT count(*) FROM s1.consumption WHERE permit_id = '" + PERMIT + "'").equals("0")) {
                connection.rollback();
                return Outcome.ALREADY_CONSUMED;
            }
            execute(connection, consumption());
            execute(connection, audit("CONSUMED"));
            connection.commit();
            if (exitAfterCommit) { return Outcome.COMMITTED_WITHOUT_SEND; }
            sends.incrementAndGet(); // local probe only; no actual executor/provider
            return Outcome.SENT;
        }
    }

    private static Outcome close(String action, Runnable afterLock, AtomicInteger pid, boolean unknown) throws SQLException {
        if (unknown) { return Outcome.HELD; }
        try (var connection = login("s1_web")) {
            connection.setAutoCommit(false);
            pid.set(Integer.parseInt(scalar(connection, "SELECT pg_backend_pid()")));
            execute(connection, "SELECT permit_id FROM s1.permit WHERE permit_id = '" + PERMIT + "' FOR UPDATE");
            afterLock.run();
            if (!scalar(connection, "SELECT count(*) FROM s1.consumption WHERE permit_id = '" + PERMIT + "'").equals("0")) {
                connection.rollback();
                return Outcome.HELD;
            }
            execute(connection, "UPDATE s1.permit SET closed_at = now(), confirmed_by = 'fixture-reviewer',"
                    + " result_ref = 'fixture-" + action + "' WHERE permit_id = '" + PERMIT + "'");
            execute(connection, audit(action));
            connection.commit();
            return Outcome.CLOSED;
        }
    }

    private static Outcome issue(String id) throws SQLException {
        try (var connection = login("s1_web")) {
            execute(connection, "INSERT INTO s1.permit (permit_id, environment_id, publication_id, event_id, listener_id,"
                    + " actor_id, reason_code, issued_at, expires_at) VALUES ('" + id + "', 'fixture-env',"
                    + " '00000000-0000-0000-0000-000000000100', '00000000-0000-0000-0000-000000000200',"
                    + " 'fixture-listener', 'fixture-actor', 'OWNER_REVIEW', now(), now() + interval '30 minutes')");
            return Outcome.ISSUED;
        } catch (SQLException error) {
            if (!"23505".equals(error.getSQLState())) { throw error; }
            System.out.println("S1-L2 expected duplicate-target SQLSTATE=23505");
            return Outcome.DUPLICATE;
        }
    }

    private static void hold(CountDownLatch locked, CountDownLatch release) {
        locked.countDown();
        try { assertTrue(release.await(10, TimeUnit.SECONDS)); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException(error); }
    }

    private static void awaitBlocked(AtomicInteger pid) throws SQLException {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        try (var observer = admin()) {
            while (System.nanoTime() < deadline) {
                int value = pid.get();
                if (value != 0 && Boolean.parseBoolean(scalar(observer,
                        "SELECT cardinality(pg_blocking_pids(" + value + ")) > 0"))) {
                    System.out.println("S1-L2 observed real PostgreSQL lock wait pid=" + value);
                    return;
                }
                LockSupport.parkNanos(Duration.ofMillis(10).toNanos());
            }
        }
        throw new AssertionError("Competing transaction never entered observed DB lock wait");
    }

    private static void assertState(int consumption, boolean closed, int audit) throws SQLException {
        try (var connection = admin()) {
            assertEquals(Integer.toString(consumption), scalar(connection, "SELECT count(*) FROM s1.consumption"));
            assertEquals(Boolean.toString(closed), scalar(connection, "SELECT closed_at IS NOT NULL FROM s1.permit"));
            assertEquals(Integer.toString(audit), scalar(connection, "SELECT count(*) FROM s1.audit_probe"));
            System.out.println("S1-L2 snapshot consumption=" + consumption + " closed=" + closed + " audit=" + audit);
        }
    }

    private static Connection admin() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static Connection login(String role) throws SQLException {
        var connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), role, "s1-fixture-only");
        execute(connection, "SET lock_timeout = '10s'");
        execute(connection, "SET statement_timeout = '10s'");
        return connection;
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) { statement.execute(sql); }
    }

    private static String scalar(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(sql)) {
            assertTrue(rows.next());
            return rows.getObject(1).toString();
        }
    }

    private static String consumption() {
        return "INSERT INTO s1.consumption VALUES ('" + PERMIT + "', '" + UUID.randomUUID() + "', 'fixture-worker', now())";
    }

    private static String audit(String action) {
        return "INSERT INTO s1.audit_probe VALUES ('" + PERMIT + "', '" + action + "')";
    }
}
