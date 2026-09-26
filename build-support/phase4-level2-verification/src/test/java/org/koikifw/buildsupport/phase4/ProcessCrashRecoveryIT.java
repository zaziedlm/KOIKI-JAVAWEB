package org.koikifw.buildsupport.phase4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Kills a separate JVM while a recorded publication is PROCESSING, then restarts it. */
class ProcessCrashRecoveryIT {

    @Test
    void incompletePublicationIsDeliveredAfterProcessRestart() throws Exception {
        crashAndRecover(false);
    }

    @Test
    void acceptedSendBeforeCrashIsNotDuplicatedAfterRestart() throws Exception {
        crashAndRecover(true);
    }

    @Test
    void concurrentRestartCanInvokeSamePublicationInTwoInstances() throws Exception {
        UUID eventId = UUID.randomUUID();
        Path work = Files.createTempDirectory(Path.of("target"), "phase4-compete-");
        Path crashedMarker = work.resolve("crashed-listener.txt");
        Path firstMarker = work.resolve("first-replay.txt");
        Path secondMarker = work.resolve("second-replay.txt");
        Path secondReady = work.resolve("second-ready.txt");
        Path jar = Path.of("target", "phase4-level2-verification-0.1.0-SNAPSHOT.jar").toAbsolutePath();
        assertTrue(Files.isRegularFile(jar), "The packaged probe JAR is required");

        try (var postgres = new PostgreSQLContainer("postgres:17-alpine")) {
            postgres.start();
            Process crashed = launch(jar, postgres, eventId, crashedMarker, null,
                    work.resolve("crashed.log"));
            try {
                await(() -> Files.exists(crashedMarker), crashed);
                assertEquals(1, publicationCount(postgres, eventId, "PROCESSING"));
            } finally {
                stop(crashed);
            }

            Process first = launch(jar, postgres, null, firstMarker, null,
                    work.resolve("first.log"));
            try {
                await(() -> Files.exists(firstMarker), first);
                Process second = launch(jar, postgres, null, secondMarker, null,
                        secondReady, work.resolve("second.log"));
                try {
                    await(() -> Files.exists(secondReady), second);
                    Thread.sleep(2_000);
                    assertTrue(Files.exists(secondMarker),
                            "The second JVM entered the same listener while the first was paused");
                    assertTrue(completionAttempts(postgres, eventId) >= 3,
                            "Both replays must be visible as completion attempts");
                    assertEquals(0, sendCount(postgres, eventId));
                } finally {
                    stop(second);
                }
            } finally {
                stop(first);
            }

            Process recovery = launch(jar, postgres, null, null, null,
                    work.resolve("recovery.log"));
            try {
                await(() -> publicationCount(postgres, eventId, "COMPLETED") == 1
                        && sendCount(postgres, eventId) == 1, recovery);
            } finally {
                stop(recovery);
            }
        } finally {
            for (String name : new String[] { "crashed-listener.txt", "first-replay.txt",
                    "second-replay.txt", "second-ready.txt", "crashed.log", "first.log",
                    "second.log", "recovery.log" }) {
                Files.deleteIfExists(work.resolve(name));
            }
            Files.deleteIfExists(work);
        }
    }

    @Test
    void dedicatedRecoveryWithAdvisoryLockPreventsConcurrentReplay() throws Exception {
        UUID eventId = UUID.randomUUID();
        Path work = Files.createTempDirectory(Path.of("target"), "phase4-exclusive-");
        Path initialMarker = work.resolve("initial-listener.txt");
        Path firstMarker = work.resolve("first-recovery-listener.txt");
        Path secondMarker = work.resolve("second-recovery-listener.txt");
        Path firstStatus = work.resolve("first-status.txt");
        Path secondStatus = work.resolve("second-status.txt");
        Path thirdStatus = work.resolve("third-status.txt");
        Path jar = Path.of("target", "phase4-level2-verification-0.1.0-SNAPSHOT.jar").toAbsolutePath();
        assertTrue(Files.isRegularFile(jar), "The packaged probe JAR is required");

        try (var postgres = new PostgreSQLContainer("postgres:17-alpine")) {
            postgres.start();
            Process initial = launch(jar, postgres, eventId, initialMarker, null,
                    work.resolve("initial.log"));
            try {
                await(() -> Files.exists(initialMarker), initial);
                assertEquals(1, publicationCount(postgres, eventId, "PROCESSING"));
            } finally {
                stop(initial);
            }

            Process first = launchRecovery(jar, postgres, eventId, firstMarker,
                    firstStatus, work.resolve("first.log"));
            try {
                await(() -> Files.exists(firstStatus), first);
                assertEquals("ACQUIRED", Files.readString(firstStatus));
                await(() -> Files.exists(firstMarker), first);
                assertEquals(2, completionAttempts(postgres, eventId));

                Process second = launchRecovery(jar, postgres, eventId, secondMarker,
                        secondStatus, work.resolve("second.log"));
                try {
                    await(() -> Files.exists(secondStatus), second);
                    assertEquals("CONTENDED", Files.readString(secondStatus));
                    assertEquals(false, Files.exists(secondMarker));
                    assertEquals(2, completionAttempts(postgres, eventId));
                    assertEquals(0, sendCount(postgres, eventId));
                } finally {
                    stop(second);
                }
            } finally {
                stop(first);
            }

            Process third = launchRecovery(jar, postgres, eventId, null,
                    thirdStatus, work.resolve("third.log"));
            try {
                await(() -> Files.exists(thirdStatus)
                        && publicationCount(postgres, eventId, "COMPLETED") == 1
                        && sendCount(postgres, eventId) == 1, third);
                assertEquals("ACQUIRED", Files.readString(thirdStatus));
                assertEquals(3, completionAttempts(postgres, eventId));
                await(() -> recoveryLockAvailable(postgres), third);
            } finally {
                stop(third);
            }
        } finally {
            for (String name : new String[] { "initial-listener.txt", "first-recovery-listener.txt",
                    "second-recovery-listener.txt", "first-status.txt", "second-status.txt",
                    "third-status.txt", "initial.log", "first.log", "second.log", "third.log" }) {
                Files.deleteIfExists(work.resolve(name));
            }
            Files.deleteIfExists(work);
        }
    }

    @Test
    void recoveryLockDoesNotProtectAnOrdinaryListenerThatIsStillRunning() throws Exception {
        UUID eventId = UUID.randomUUID();
        Path work = Files.createTempDirectory(Path.of("target"), "phase4-live-listener-");
        Path ordinaryMarker = work.resolve("ordinary-listener.txt");
        Path recoveryMarker = work.resolve("recovery-listener.txt");
        Path recoveryStatus = work.resolve("recovery-status.txt");
        Path finalStatus = work.resolve("final-status.txt");
        Path jar = Path.of("target", "phase4-level2-verification-0.1.0-SNAPSHOT.jar").toAbsolutePath();
        assertTrue(Files.isRegularFile(jar), "The packaged probe JAR is required");

        try (var postgres = new PostgreSQLContainer("postgres:17-alpine")) {
            postgres.start();
            Process ordinary = launch(jar, postgres, eventId, ordinaryMarker, null,
                    work.resolve("ordinary.log"));
            try {
                await(() -> Files.exists(ordinaryMarker), ordinary);
                assertEquals(1, publicationCount(postgres, eventId, "PROCESSING"));

                Process recovery = launchRecovery(jar, postgres, eventId, recoveryMarker,
                        recoveryStatus, work.resolve("recovery.log"));
                try {
                    await(() -> Files.exists(recoveryStatus) && Files.exists(recoveryMarker), recovery);
                    assertEquals("ACQUIRED", Files.readString(recoveryStatus));
                    assertEquals(2, completionAttempts(postgres, eventId));
                    assertEquals(0, sendCount(postgres, eventId));
                } finally {
                    stop(recovery);
                }
            } finally {
                stop(ordinary);
            }

            Process finalRecovery = launchRecovery(jar, postgres, eventId, null,
                    finalStatus, work.resolve("final.log"));
            try {
                await(() -> Files.exists(finalStatus)
                        && publicationCount(postgres, eventId, "COMPLETED") == 1
                        && sendCount(postgres, eventId) == 1, finalRecovery);
                assertEquals("ACQUIRED", Files.readString(finalStatus));
            } finally {
                stop(finalRecovery);
            }
        } finally {
            for (String name : new String[] { "ordinary-listener.txt", "recovery-listener.txt",
                    "recovery-status.txt", "final-status.txt", "ordinary.log", "recovery.log",
                    "final.log" }) {
                Files.deleteIfExists(work.resolve(name));
            }
            Files.deleteIfExists(work);
        }
    }

    @Test
    void lostAdvisoryLockConnectionAllowsAnotherRecoveryWhileListenerIsRunning() throws Exception {
        UUID eventId = UUID.randomUUID();
        Path work = Files.createTempDirectory(Path.of("target"), "phase4-lock-loss-");
        Path initialMarker = work.resolve("initial-listener.txt");
        Path firstMarker = work.resolve("first-listener.txt");
        Path secondMarker = work.resolve("second-listener.txt");
        Path firstStatus = work.resolve("first-status.txt");
        Path secondStatus = work.resolve("second-status.txt");
        Path finalStatus = work.resolve("final-status.txt");
        Path jar = Path.of("target", "phase4-level2-verification-0.1.0-SNAPSHOT.jar").toAbsolutePath();
        assertTrue(Files.isRegularFile(jar), "The packaged probe JAR is required");

        try (var postgres = new PostgreSQLContainer("postgres:17-alpine")) {
            postgres.start();
            Process initial = launch(jar, postgres, eventId, initialMarker, null,
                    work.resolve("initial.log"));
            try {
                await(() -> Files.exists(initialMarker), initial);
            } finally {
                stop(initial);
            }

            Process first = launchRecovery(jar, postgres, eventId, firstMarker,
                    firstStatus, work.resolve("first.log"));
            try {
                await(() -> Files.exists(firstStatus) && Files.exists(firstMarker), first);
                assertEquals("ACQUIRED", Files.readString(firstStatus));
                assertEquals(2, completionAttempts(postgres, eventId));

                assertEquals(1, terminateRecoveryLockBackend(postgres));
                await(() -> recoveryLockAvailable(postgres), first);
                assertTrue(first.isAlive(), "The first recovery JVM still has an active listener");

                Process second = launchRecovery(jar, postgres, eventId, secondMarker,
                        secondStatus, work.resolve("second.log"));
                try {
                    await(() -> Files.exists(secondStatus) && Files.exists(secondMarker), second);
                    assertEquals("ACQUIRED", Files.readString(secondStatus));
                    assertTrue(completionAttempts(postgres, eventId) >= 3);
                    assertEquals(0, sendCount(postgres, eventId));
                } finally {
                    stop(second);
                }
            } finally {
                stop(first);
            }

            Process finalRecovery = launchRecovery(jar, postgres, eventId, null,
                    finalStatus, work.resolve("final.log"));
            try {
                await(() -> Files.exists(finalStatus)
                        && publicationCount(postgres, eventId, "COMPLETED") == 1
                        && sendCount(postgres, eventId) == 1, finalRecovery);
                assertEquals("ACQUIRED", Files.readString(finalStatus));
            } finally {
                stop(finalRecovery);
            }
        } finally {
            for (String name : new String[] { "initial-listener.txt", "first-listener.txt",
                    "second-listener.txt", "first-status.txt", "second-status.txt",
                    "final-status.txt", "initial.log", "first.log", "second.log", "final.log" }) {
                Files.deleteIfExists(work.resolve(name));
            }
            Files.deleteIfExists(work);
        }
    }

    private static void crashAndRecover(boolean afterSend) throws Exception {
        UUID eventId = UUID.randomUUID();
        Path work = Files.createTempDirectory(Path.of("target"), "phase4-crash-");
        Path marker = work.resolve("listener-entered.txt");
        Path firstLog = work.resolve("first.log");
        Path secondLog = work.resolve("second.log");
        Path jar = Path.of("target", "phase4-level2-verification-0.1.0-SNAPSHOT.jar").toAbsolutePath();
        assertTrue(Files.isRegularFile(jar), "The packaged probe JAR is required");

        try (var postgres = new PostgreSQLContainer("postgres:17-alpine")) {
            postgres.start();
            Process first = launch(jar, postgres, eventId,
                    afterSend ? null : marker, afterSend ? marker : null, firstLog);
            try {
                await(() -> Files.exists(marker), first);
                assertEquals(1, publicationCount(postgres, eventId, "PROCESSING"));
                assertEquals(afterSend ? 1 : 0, sendCount(postgres, eventId));
            } finally {
                stop(first);
            }

            Process second = launch(jar, postgres, null, null, null, secondLog);
            try {
                await(() -> sendCount(postgres, eventId) == 1
                        && publicationCount(postgres, eventId, "COMPLETED") == 1, second);
            } finally {
                stop(second);
            }
        } finally {
            Files.deleteIfExists(marker);
            Files.deleteIfExists(firstLog);
            Files.deleteIfExists(secondLog);
            Files.deleteIfExists(work);
        }
    }

    private static Process launch(
            Path jar, PostgreSQLContainer postgres, UUID eventId,
            Path pauseBeforeSend, Path pauseAfterSend, Path log)
            throws IOException {
        return launch(jar, postgres, eventId, pauseBeforeSend, pauseAfterSend, null, log);
    }

    private static Process launch(
            Path jar, PostgreSQLContainer postgres, UUID eventId,
            Path pauseBeforeSend, Path pauseAfterSend, Path ready, Path log)
            throws IOException {
        String java = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
        var command = new java.util.ArrayList<String>();
        command.add(java);
        command.add("-jar");
        command.add(jar.toString());
        command.add("--probe.keep-alive=true");
        command.add("--spring.modulith.events.republish-outstanding-events-on-restart=true");
        if (eventId != null) {
            command.add("--probe.approve.id=" + eventId);
        }
        if (pauseBeforeSend != null) {
            command.add("--probe.pause.file=" + pauseBeforeSend.toAbsolutePath());
        }
        if (pauseAfterSend != null) {
            command.add("--probe.pause.after-send.file=" + pauseAfterSend.toAbsolutePath());
        }
        if (ready != null) {
            command.add("--probe.ready.file=" + ready.toAbsolutePath());
        }
        var builder = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("SPRING_DATASOURCE_URL", postgres.getJdbcUrl());
        builder.environment().put("SPRING_DATASOURCE_USERNAME", postgres.getUsername());
        builder.environment().put("SPRING_DATASOURCE_PASSWORD", postgres.getPassword());
        return builder.start();
    }

    private static Process launchRecovery(
            Path jar, PostgreSQLContainer postgres, UUID eventId,
            Path pauseBeforeSend, Path status, Path log) throws IOException {
        String java = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
        var command = new java.util.ArrayList<String>();
        command.add(java);
        command.add("-jar");
        command.add(jar.toString());
        command.add("--probe.keep-alive=true");
        command.add("--spring.modulith.events.republish-outstanding-events-on-restart=false");
        command.add("--probe.recover.id=" + eventId);
        command.add("--probe.recover.status.file=" + status.toAbsolutePath());
        if (pauseBeforeSend != null) {
            command.add("--probe.pause.file=" + pauseBeforeSend.toAbsolutePath());
        }
        var builder = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("SPRING_DATASOURCE_URL", postgres.getJdbcUrl());
        builder.environment().put("SPRING_DATASOURCE_USERNAME", postgres.getUsername());
        builder.environment().put("SPRING_DATASOURCE_PASSWORD", postgres.getPassword());
        return builder.start();
    }

    private static int publicationCount(PostgreSQLContainer postgres, UUID id, String status) {
        return count(postgres, """
                SELECT count(*) FROM event_publication
                WHERE serialized_event LIKE ? AND status = ?
                """, "%" + id + "%", status);
    }

    private static int sendCount(PostgreSQLContainer postgres, UUID id) {
        return count(postgres,
                "SELECT count(*) FROM probe_provider_send WHERE event_id = ?", id);
    }

    private static int completionAttempts(PostgreSQLContainer postgres, UUID id) {
        return count(postgres, """
                SELECT completion_attempts FROM event_publication
                WHERE serialized_event LIKE ?
                """, "%" + id + "%");
    }

    private static boolean recoveryLockAvailable(PostgreSQLContainer postgres) {
        try (var connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var acquire = connection.prepareStatement("SELECT pg_try_advisory_lock(?, ?)")) {
            acquire.setInt(1, ExclusiveRecoveryProbe.LOCK_NAMESPACE);
            acquire.setInt(2, ExclusiveRecoveryProbe.LOCK_TASK);
            try (var rows = acquire.executeQuery()) {
                rows.next();
                if (!rows.getBoolean(1)) {
                    return false;
                }
            }
            try (var release = connection.prepareStatement("SELECT pg_advisory_unlock(?, ?)")) {
                release.setInt(1, ExclusiveRecoveryProbe.LOCK_NAMESPACE);
                release.setInt(2, ExclusiveRecoveryProbe.LOCK_TASK);
                release.executeQuery().close();
            }
            return true;
        } catch (SQLException exception) {
            throw new IllegalStateException("PL2 recovery lock observation failed", exception);
        }
    }

    private static int terminateRecoveryLockBackend(PostgreSQLContainer postgres) {
        try (var connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.prepareStatement("""
                        SELECT pg_terminate_backend(pid) FROM pg_locks
                        WHERE locktype = 'advisory' AND granted
                          AND classid::bigint = ? AND objid::bigint = ?
                        """)) {
            statement.setLong(1, ExclusiveRecoveryProbe.LOCK_NAMESPACE);
            statement.setLong(2, ExclusiveRecoveryProbe.LOCK_TASK);
            int terminated = 0;
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    assertTrue(rows.getBoolean(1), "The lock-owning backend was not terminated");
                    terminated++;
                }
            }
            return terminated;
        } catch (SQLException exception) {
            throw new IllegalStateException("PL2 recovery lock termination failed", exception);
        }
    }

    private static int count(PostgreSQLContainer postgres, String sql, Object... args) {
        try (var connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }
            try (var rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("PL2 database observation failed", exception);
        }
    }

    private static void await(BooleanSupplier condition, Process process) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            assertTrue(process.isAlive(), "Probe JVM exited before the expected state");
            Thread.sleep(100);
        }
        assertTrue(condition.getAsBoolean(), "Expected publication state did not appear");
    }

    private static void stop(Process process) throws InterruptedException {
        if (process.isAlive()) {
            process.destroyForcibly();
        }
        assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Probe JVM did not stop");
    }
}
