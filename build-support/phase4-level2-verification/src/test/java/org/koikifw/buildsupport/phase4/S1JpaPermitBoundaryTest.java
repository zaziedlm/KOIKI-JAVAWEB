package org.koikifw.buildsupport.phase4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.LockModeType;
import jakarta.persistence.Entity;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.koikifw.buildsupport.phase4.s1fixture.S1JpaFixture;
import org.koikifw.buildsupport.phase4.s1fixture.S1JpaFixture.Consumption;
import org.koikifw.buildsupport.phase4.s1fixture.S1JpaFixture.Permit;
import org.koikifw.buildsupport.phase4.s1fixture.S1JpaFixture.Tx;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** A: actual JPA SQL under restricted roles. No Identity, Audit or external provider. */
@Timeout(600)
class S1JpaPermitBoundaryTest {
    private static final UUID PERMIT = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PUBLICATION = UUID.fromString("00000000-0000-0000-0000-000000000100");
    private static final UUID OPERATION = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final String IMAGE = "postgres@sha256:"
            + "18cfe3ef5e6815560c98237d6216d1e5119702fb0f3894c8785dd58b8bbe5d73";
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(IMAGE)
            .withCommand("postgres", "-c", "max_connections=16")
            .withCreateContainerCmdModifier(command -> command.getHostConfig()
                    .withMemory(1024L * 1024 * 1024).withNanoCPUs(1_000_000_000L));
    private static S1JpaFixture web;
    private static S1JpaFixture worker;
    private static S1JpaFixture reader;
    private final AtomicInteger sends = new AtomicInteger();

    @BeforeAll
    static void startDatabase() throws SQLException {
        assertFalse(Permit.class.isAnnotationPresent(Entity.class));
        assertFalse(Consumption.class.isAnnotationPresent(Entity.class));
        POSTGRES.start();
        try {
            try (var connection = admin()) {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("s1-additional/permit-jpa.sql"));
                assertEquals("16", scalar(connection, "SHOW max_connections"));
                System.out.println("S1-A database=" + scalar(connection, "SHOW server_version")
                        + " container=" + POSTGRES.getContainerId() + " image=" + IMAGE);
            }
            assertEquals(1024L * 1024 * 1024, POSTGRES.getContainerInfo().getHostConfig().getMemory());
            assertEquals(1_000_000_000L, POSTGRES.getContainerInfo().getHostConfig().getNanoCPUs());
            web = new S1JpaFixture(POSTGRES.getJdbcUrl(), "s1a_web", true);
            worker = new S1JpaFixture(POSTGRES.getJdbcUrl(), "s1a_worker", true);
            reader = new S1JpaFixture(POSTGRES.getJdbcUrl(), "s1a_reader", false);
            for (var role : List.of(web, worker, reader)) {
                try (var tx = role.begin()) {
                    String login = tx.em().createNativeQuery("SELECT current_user").getSingleResult().toString();
                    assertTrue(login.startsWith("s1a_"));
                    assertFalse(login.equals("s1a_owner"));
                    System.out.println("S1-A runtime-login=" + login);
                }
            }
        } catch (RuntimeException | Error | SQLException failure) {
            stopDatabase();
            throw failure;
        }
    }

    @AfterAll
    static void stopDatabase() {
        try {
            if (reader != null) { reader.close(); reader = null; }
            if (worker != null) { worker.close(); worker = null; }
            if (web != null) { web.close(); web = null; }
        } finally { POSTGRES.stop(); }
    }

    @BeforeEach
    void seed() throws SQLException {
        sends.set(0);
        try (var connection = admin()) { execute(connection, "TRUNCATE s1a.consumption, s1a.permit"); }
        issue(PERMIT, PUBLICATION);
        web.clearSql(); worker.clearSql(); reader.clearSql();
        System.out.println("S1-A before=" + snapshot());
    }

    @AfterEach
    void recordAfter() throws SQLException {
        try (var connection = admin()) {
            int connections = Integer.parseInt(scalar(connection,
                    "SELECT count(*) FROM pg_stat_activity WHERE datname = current_database()"));
            assertTrue(connections <= 8, "DB simultaneous connection ceiling");
            System.out.println("S1-A connections=" + connections + " after=" + snapshot() + " sends=" + sends.get());
        }
    }

    @Test
    void a1_01_webJpaInsertAndCommitReread() throws SQLException {
        try (var connection = admin()) { execute(connection, "TRUNCATE s1a.consumption, s1a.permit"); }
        try (var tx = web.begin(); var observer = login("s1a_reader")) {
            tx.em().persist(new Permit(PERMIT, PUBLICATION));
            tx.em().flush();
            assertEquals("0", scalar(observer, "SELECT count(*) FROM s1a.permit"));
            tx.commit();
            assertEquals("1", scalar(observer, "SELECT count(*) FROM s1a.permit"));
        }
        try (var tx = reader.begin()) {
            var value = Objects.requireNonNull(tx.em().find(Permit.class, PERMIT));
            assertEquals(PUBLICATION, value.publication());
            assertEquals("fixture-actor", value.actor());
            assertEquals(S1JpaFixture.ISSUED, value.issued());
            assertEquals(S1JpaFixture.EXPIRES, value.expires());
            assertEquals(0L, value.version());
            assertFalse(value.closed());
        }
        String insert = web.sql().stream().filter(sql -> sql.startsWith("insert into s1a.permit")).findFirst().orElseThrow();
        for (String forbidden : List.of("closed_at", "confirmed_by", "result_ref", "version")) {
            assertFalse(insert.contains(forbidden), insert);
        }
        assertEquals(0, sends.get());
    }

    @Test
    void a2_01_workerLocksAndUpdatesOnlyVersion() throws SQLException {
        try (var tx = worker.begin()) {
            var value = Objects.requireNonNull(tx.em().find(Permit.class, PERMIT, LockModeType.PESSIMISTIC_FORCE_INCREMENT));
            tx.em().flush();
            assertEquals(1L, value.version());
            tx.commit();
        }
        assertEquals("1", db("SELECT version FROM s1a.permit"));
        assertTrue(worker.sql().stream().anyMatch(sql -> sql.contains("for no key update") || sql.contains("for update")));
        var updates = worker.sql().stream().filter(sql -> sql.startsWith("update s1a.permit")).toList();
        assertFalse(updates.isEmpty());
        for (String sql : updates) { assertTrue(sql.contains("set version=? where"), sql); }
    }

    @Test
    void a2_02_webJpaClosureFlushOmitsImmutableColumns() throws SQLException {
        assertEquals("CLOSED", close("CONFIRM", () -> {}, new AtomicInteger()));
        assertEquals("true", db("SELECT closed_at IS NOT NULL FROM s1a.permit"));
        assertEquals("1", db("SELECT version FROM s1a.permit"));
        assertEquals("fixture-reviewer", db("SELECT confirmed_by FROM s1a.permit"));
        assertEquals("fixture-actor", db("SELECT actor_id FROM s1a.permit"));
        String sql = web.sql().stream().filter(value -> value.startsWith("update s1a.permit")).findFirst().orElseThrow();
        String assignments = sql.substring(sql.indexOf(" set "), sql.indexOf(" where "));
        for (String forbidden : List.of("actor_id", "publication_id", "issued_at", "expires_at", "environment_id")) {
            assertFalse(assignments.contains(forbidden), sql);
        }
        assertTrue(assignments.contains("closed_at") && assignments.contains("confirmed_by")
                && assignments.contains("result_ref") && assignments.contains("version"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"permit", "operation"})
    void a3_01_consumptionJpaInsertRejectsReuse(String uniqueKey) throws SQLException {
        insertConsumption(PERMIT, OPERATION);
        UUID nextPermit = PERMIT;
        UUID nextOperation = UUID.randomUUID();
        if (uniqueKey.equals("operation")) {
            nextPermit = UUID.randomUUID();
            nextOperation = OPERATION;
            issue(nextPermit, UUID.randomUUID());
        }
        UUID duplicatePermit = nextPermit;
        UUID duplicateOperation = nextOperation;
        String before = snapshot();
        rejected("23505", () -> insertConsumption(duplicatePermit, duplicateOperation));
        assertEquals(before, snapshot());
        assertEquals("1", db("SELECT count(*) FROM s1a.consumption"));
        assertEquals(0, sends.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"web", "worker"})
    void a3_02_immutableColumnsRejectActualJpaSql(String role) throws SQLException {
        var fixture = role.equals("web") ? web : worker;
        String before = snapshot();
        for (String assignment : List.of("publication_id = gen_random_uuid()", "event_id = gen_random_uuid()",
                "environment_id = 'other'", "listener_id = 'other'", "actor_id = 'forged'",
                "reason_code = 'forged'", "issued_at = now()", "expires_at = now() + interval '1 day'")) {
            rejected("42501", () -> {
                try (var tx = fixture.begin()) {
                    tx.em().createNativeQuery("UPDATE s1a.permit SET " + assignment).executeUpdate();
                    tx.commit();
                }
            });
            assertEquals(before, snapshot());
        }
        assertEquals(0, sends.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"UPDATE", "DELETE", "TRUNCATE"})
    void a3_03_appendOnlyConsumptionRejectsMutation(String action) throws SQLException {
        insertConsumption(PERMIT, OPERATION);
        String before = snapshot();
        rejected("42501", () -> {
            try (var tx = worker.begin()) {
                if (action.equals("TRUNCATE")) {
                    tx.em().createNativeQuery("TRUNCATE s1a.consumption").executeUpdate();
                } else {
                    var value = Objects.requireNonNull(tx.em().find(Consumption.class, PERMIT));
                    if (action.equals("UPDATE")) { value.corruptForNegativeTest(); }
                    else { tx.em().remove(value); }
                    tx.em().flush();
                }
                tx.commit();
            }
        });
        assertEquals(before, snapshot());
        assertEquals(0, sends.get());
    }

    @Test
    void a4_01_samePermitJpaConsumptionHasOneSender() throws Exception {
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { barrier.await(10, TimeUnit.SECONDS); return consume(() -> {}, new AtomicInteger()); });
            var second = executor.submit(() -> { barrier.await(10, TimeUnit.SECONDS); return consume(() -> {}, new AtomicInteger()); });
            var outcomes = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertTrue(outcomes.contains("SENT") && outcomes.contains("ALREADY_CONSUMED"), outcomes.toString());
            System.out.println("S1-A concurrent-consume=" + outcomes);
        }
        assertEquals("1", db("SELECT count(*) FROM s1a.consumption"));
        assertEquals(1, sends.get());
    }

    @Test
    void a4_02_sameTargetJpaIssueHasOneWinner() throws Exception {
        try (var connection = admin()) { execute(connection, "TRUNCATE s1a.consumption, s1a.permit"); }
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> { barrier.await(10, TimeUnit.SECONDS); return competingIssue(); });
            var second = executor.submit(() -> { barrier.await(10, TimeUnit.SECONDS); return competingIssue(); });
            var outcomes = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertTrue(outcomes.contains("ISSUED") && outcomes.contains("DUPLICATE"), outcomes.toString());
            System.out.println("S1-A concurrent-issue=" + outcomes);
        }
        assertEquals("1", db("SELECT count(*) FROM s1a.permit"));
        assertEquals(0, sends.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONFIRM", "CANCEL"})
    void a4_03_consumptionFirstHoldsClosure(String action) throws Exception {
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var waiter = new AtomicInteger();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> consume(() -> hold(locked, release), new AtomicInteger()));
            try {
                assertTrue(locked.await(10, TimeUnit.SECONDS));
                var second = executor.submit(() -> close(action, () -> {}, waiter));
                awaitBlocked(waiter);
                release.countDown();
                assertEquals("SENT", first.get(10, TimeUnit.SECONDS));
                assertEquals("HELD", second.get(10, TimeUnit.SECONDS));
            } finally { release.countDown(); }
        }
        assertEquals("false", db("SELECT closed_at IS NOT NULL FROM s1a.permit"));
        assertEquals("1", db("SELECT count(*) FROM s1a.consumption"));
        assertEquals(1, sends.get());
        rejected("23505", () -> issue(UUID.randomUUID(), PUBLICATION));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONFIRM", "CANCEL"})
    void a4_04_closureFirstRefusesConsumption(String action) throws Exception {
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var waiter = new AtomicInteger();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> close(action, () -> hold(locked, release), new AtomicInteger()));
            try {
                assertTrue(locked.await(10, TimeUnit.SECONDS));
                var second = executor.submit(() -> consume(() -> {}, waiter));
                awaitBlocked(waiter);
                release.countDown();
                assertEquals("CLOSED", first.get(10, TimeUnit.SECONDS));
                assertEquals("CLOSED", second.get(10, TimeUnit.SECONDS));
            } finally { release.countDown(); }
        }
        assertEquals("true", db("SELECT closed_at IS NOT NULL FROM s1a.permit"));
        assertEquals("0", db("SELECT count(*) FROM s1a.consumption"));
        assertEquals(0, sends.get());
    }

    @Test
    void a4_05_flushedJpaConsumptionRollsBack() throws SQLException {
        String before = snapshot();
        try (var tx = worker.begin(); var observer = login("s1a_reader")) {
            tx.em().find(Permit.class, PERMIT, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
            tx.em().persist(new Consumption(PERMIT, OPERATION));
            tx.em().flush();
            assertEquals("0", scalar(observer, "SELECT count(*) FROM s1a.consumption"));
            assertEquals("0", scalar(observer, "SELECT version FROM s1a.permit"));
            tx.rollback();
        }
        assertEquals(before, snapshot());
        assertEquals(0, sends.get());
    }

    @Test
    void a5_01_readerJpaReadButNoLockOrUpdate() throws SQLException {
        String before = snapshot();
        try (var tx = reader.begin()) {
            assertEquals("fixture-actor", Objects.requireNonNull(tx.em().find(Permit.class, PERMIT)).actor());
        }
        rejected("42501", () -> {
            try (var tx = reader.begin()) { tx.em().find(Permit.class, PERMIT, LockModeType.PESSIMISTIC_WRITE); }
        });
        rejected("42501", () -> {
            try (var tx = reader.begin()) {
                var value = Objects.requireNonNull(tx.em().find(Permit.class, PERMIT));
                tx.em().lock(value, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
                tx.commit();
            }
        });
        assertEquals(before, snapshot());
        assertEquals(0, sends.get());
    }

    private String consume(Runnable afterLock, AtomicInteger pid) {
        try (var tx = worker.begin()) {
            pid.set(backendPid(tx));
            var permit = Objects.requireNonNull(tx.em().find(Permit.class, PERMIT, LockModeType.PESSIMISTIC_WRITE));
            afterLock.run();
            if (permit.closed()) { return "CLOSED"; }
            if (tx.em().find(Consumption.class, PERMIT) != null) { return "ALREADY_CONSUMED"; }
            tx.em().lock(permit, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
            tx.em().persist(new Consumption(PERMIT, UUID.randomUUID()));
            tx.commit();
            sends.incrementAndGet(); // commit success before local probe; never actual provider delivery
            return "SENT";
        }
    }

    private static String close(String action, Runnable afterLock, AtomicInteger pid) {
        try (var tx = web.begin()) {
            pid.set(backendPid(tx));
            var permit = Objects.requireNonNull(tx.em().find(Permit.class, PERMIT, LockModeType.PESSIMISTIC_WRITE));
            afterLock.run();
            if (tx.em().find(Consumption.class, PERMIT) != null) { return "HELD"; }
            permit.confirm(action);
            tx.commit();
            return "CLOSED";
        }
    }

    private static void insertConsumption(UUID permit, UUID operation) {
        try (var tx = worker.begin()) { tx.em().persist(new Consumption(permit, operation)); tx.commit(); }
    }

    private static void issue(UUID id, UUID publication) {
        try (var tx = web.begin()) { tx.em().persist(new Permit(id, publication)); tx.commit(); }
    }

    private static String competingIssue() {
        try { issue(UUID.randomUUID(), PUBLICATION); return "ISSUED"; }
        catch (RuntimeException failure) {
            assertEquals("23505", sqlState(failure));
            System.out.println("S1-A expected target duplicate SQLSTATE=23505");
            return "DUPLICATE";
        }
    }

    private static int backendPid(Tx tx) {
        return ((Number) tx.em().createNativeQuery("SELECT pg_backend_pid()").getSingleResult()).intValue();
    }

    private static void rejected(String state, Runnable operation) {
        var failure = assertThrows(RuntimeException.class, operation::run);
        assertEquals(state, sqlState(failure));
        System.out.println("S1-A expected SQLSTATE=" + state);
    }

    private static String sqlState(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql) { return Objects.requireNonNull(sql.getSQLState()); }
        }
        throw new AssertionError("No SQLSTATE in JPA failure", error);
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
                if (pid.get() != 0 && Boolean.parseBoolean(scalar(observer,
                        "SELECT cardinality(pg_blocking_pids(" + pid.get() + ")) > 0"))) {
                    int connections = Integer.parseInt(scalar(observer,
                            "SELECT count(*) FROM pg_stat_activity WHERE datname = current_database()"));
                    assertTrue(connections <= 8);
                    System.out.println("S1-A observed DB lock wait pid=" + pid.get() + " connections=" + connections);
                    return;
                }
                LockSupport.parkNanos(Duration.ofMillis(10).toNanos());
            }
        }
        throw new AssertionError("No observed DB lock wait");
    }

    private static Connection admin() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static Connection login(String role) throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), role, "s1-fixture-only");
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

    private static String db(String sql) throws SQLException {
        try (var connection = login("s1a_reader")) { return scalar(connection, sql); }
    }

    private static String snapshot() throws SQLException {
        try (var connection = login("s1a_reader")) {
            return scalar(connection, "SELECT jsonb_build_object('permits',"
                    + " COALESCE((SELECT jsonb_agg(to_jsonb(p) ORDER BY permit_id) FROM s1a.permit p), '[]'::jsonb),"
                    + " 'consumptions', COALESCE((SELECT jsonb_agg(to_jsonb(c) ORDER BY permit_id)"
                    + " FROM s1a.consumption c), '[]'::jsonb))");
        }
    }
}
