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
