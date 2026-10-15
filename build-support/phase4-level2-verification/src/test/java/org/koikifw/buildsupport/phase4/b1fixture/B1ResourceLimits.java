package org.koikifw.buildsupport.phase4.b1fixture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Opt-in, test-only limits. No fixture business assertion or ordinary launch is replaced. */
public final class B1ResourceLimits {
    private static final long HEAP = 805306368L;
    private static Process child;

    private B1ResourceLimits() { }

    public static boolean enabled() {
        String value = System.getProperty("koiki.b1.resource-limits.enabled", "false");
        if (!value.equals("true") && !value.equals("false")) {
            throw new IllegalArgumentException("Invalid B1 resource limits selection");
        }
        return value.equals("true");
    }

    public static PostgreSQLContainer container() {
        var postgres = new PostgreSQLContainer("postgres:17-alpine");
        if (enabled()) {
            assertEquals(HEAP, Runtime.getRuntime().maxMemory(), "B1 test fork heap");
            postgres.withCommand("postgres", "-c", "max_connections=16", "-c", "lock_timeout=10s",
                    "-c", "statement_timeout=10s", "-c", "transaction_timeout=10s")
                    .withCreateContainerCmdModifier(command -> Objects.requireNonNull(command.getHostConfig())
                            .withMemory(1073741824L).withNanoCPUs(1000000000L));
        }
        return postgres;
    }

    public static void properties(DynamicPropertyRegistry registry) {
        if (!enabled()) return;
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "2");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "0");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "10000");
        registry.add("spring.transaction.default-timeout", () -> "10s");
    }

    public static void assertPool(HikariDataSource pool) {
        if (!enabled()) return;
        assertEquals(2, pool.getMaximumPoolSize());
        assertEquals(0, pool.getMinimumIdle());
        assertEquals(10000L, pool.getConnectionTimeout());
        System.out.println("B1_RESOURCE poolMax=2 idle=0 connectionTimeoutMs=10000");
    }

    public static void assertDatabase(PostgreSQLContainer postgres) throws SQLException {
        if (!enabled()) return;
        var host = Objects.requireNonNull(postgres.getContainerInfo().getHostConfig());
        assertEquals(Long.valueOf(1073741824L), host.getMemory());
        assertEquals(Long.valueOf(1000000000L), host.getNanoCPUs());
        String url = postgres.getJdbcUrl();
        url += (url.contains("?") ? "&" : "?") + "connectTimeout=10&socketTimeout=10";
        try (var connection = DriverManager.getConnection(url, postgres.getUsername(), postgres.getPassword());
                var statement = connection.createStatement()) {
            statement.setQueryTimeout(10);
            for (String setting : new String[] { "max_connections", "lock_timeout", "statement_timeout", "transaction_timeout" }) {
                try (var result = statement.executeQuery("SHOW " + setting)) {
                    assertTrue(result.next());
                    assertEquals(setting.equals("max_connections") ? "16" : "10s", result.getString(1));
                }
            }
            try (var result = statement.executeQuery("SELECT count(*) FROM pg_stat_activity WHERE backend_type='client backend'")) {
                assertTrue(result.next());
                int connections = result.getInt(1);
                assertTrue(connections <= 8, "B1 connection budget exceeded");
                System.out.println("B1_RESOURCE clientConnections=" + connections);
            }
        }
        System.out.println("B1_RESOURCE dbId=" + postgres.getContainerId()
                + " memory=1073741824 nanoCpus=1000000000 maxConnections=16 dbTimeoutSeconds=10 heap=" + HEAP);
    }

    /** Checks the selected child's actual VM flags and Hikari configuration before returning it. */
    public static synchronized Process start(ProcessBuilder builder, PostgreSQLContainer postgres, Path log)
            throws IOException, InterruptedException, SQLException {
        if (!enabled()) return builder.start();
        assertTrue(child == null || !child.isAlive(), "B1 allows one child JVM at a time");
        builder.command().add(1, "-Xmx768m");
        builder.environment().remove("SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE");
        builder.environment().remove("SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE");
        builder.environment().remove("SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT");
        builder.environment().remove("SPRING_DATASOURCE_HIKARI_LEAK_DETECTION_THRESHOLD");
        builder.command().add("--spring.datasource.hikari.maximum-pool-size=2");
        builder.command().add("--spring.datasource.hikari.minimum-idle=0");
        builder.command().add("--spring.datasource.hikari.connection-timeout=10000");
        builder.command().add("--spring.datasource.hikari.leak-detection-threshold=2000");
        builder.environment().put("SPRING_TRANSACTION_DEFAULT_TIMEOUT", "10s");
        builder.command().add("--logging.level.com.zaxxer.hikari=DEBUG");
        builder.command().add("--logging.level.org.flywaydb=DEBUG");
        long launchedAt = System.nanoTime();
        Process started = builder.start();
        child = started;
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
            String contents = "";
            boolean observed = false;
            while (started.isAlive() && System.nanoTime() < deadline) {
                contents = Files.exists(log) ? Files.readString(log) : "";
                if (!observed && (System.nanoTime() - launchedAt >= TimeUnit.SECONDS.toNanos(4)
                        || contents.contains("Started ProbeApplication"))) {
                    observeStartup(postgres, started.pid(), launchedAt);
                    observed = true;
                }
                if (contents.contains("Started ProbeApplication")) break;
                Thread.sleep(100);
            }
            assertTrue(started.isAlive() && contents.contains("Started ProbeApplication"), "B1 child startup failed");
            assertTrue(contents.matches("(?s).*maximumPoolSize\\s*\\.+2\\s.*"), "B1 child pool maximum");
            assertTrue(contents.matches("(?s).*minimumIdle\\s*\\.+0\\s.*"), "B1 child pool idle");
            assertTrue(contents.matches("(?s).*connectionTimeout\\s*\\.+10000\\s.*"), "B1 child connection timeout");
            var jcmd = Path.of(System.getProperty("java.home"), "bin", "jcmd.exe");
            Process inspect = new ProcessBuilder(jcmd.toString(), Long.toString(started.pid()), "VM.flags")
                    .redirectErrorStream(true).start();
            try {
                assertTrue(inspect.waitFor(10, TimeUnit.SECONDS), "B1 VM inspection timeout");
                String flags = new String(inspect.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                assertEquals(0, inspect.exitValue());
                assertTrue(flags.contains("-XX:MaxHeapSize=" + HEAP), "B1 child actual heap");
            } finally {
                if (inspect.isAlive()) inspect.destroyForcibly();
                assertTrue(inspect.waitFor(10, TimeUnit.SECONDS), "B1 VM inspector cleanup");
            }
            assertDatabase(postgres);
            System.out.println("B1_RESOURCE childPid=" + started.pid() + " heap=" + HEAP
                    + " poolMax=2 idle=0 connectionTimeoutMs=10000 concurrentChildren=1");
            return started;
        } catch (IOException | InterruptedException | SQLException | RuntimeException | AssertionError failure) {
            boolean aliveAtFailure = started.isAlive();
            Integer exitAtFailure = aliveAtFailure ? null : started.exitValue();
            started.destroyForcibly();
            assertTrue(started.waitFor(10, TimeUnit.SECONDS), "B1 failed child cleanup");
            try {
                preserveFailure(builder, started, log, launchedAt, aliveAtFailure, exitAtFailure);
            } catch (IOException | RuntimeException diagnosticFailure) {
                failure.addSuppressed(diagnosticFailure);
            }
            throw failure;
        }
    }

    /** One read-only management connection; never records SQL text or connection credentials. */
    private static void observeStartup(PostgreSQLContainer postgres, long pid, long launchedAt)
            throws SQLException, IOException {
        String url = postgres.getJdbcUrl();
        url += (url.contains("?") ? "&" : "?") + "connectTimeout=10&socketTimeout=10";
        var observation = new StringBuilder("observedAt=" + java.time.Instant.now()
                + " childPid=" + pid + " elapsedMs="
                + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - launchedAt) + "\n");
        int count = 0;
        try (var connection = DriverManager.getConnection(url, postgres.getUsername(), postgres.getPassword());
                var statement = connection.createStatement()) {
            connection.setReadOnly(true);
            statement.setQueryTimeout(10);
            try (var result = statement.executeQuery("""
                    SELECT pid, state, wait_event_type, wait_event,
                        CASE WHEN pid = pg_backend_pid() THEN 'MANAGEMENT_OBSERVER'
                             WHEN query ~* 'pg_advisory(_xact)?_lock' THEN 'ADVISORY_LOCK'
                             WHEN query ~* 'flyway_schema_history' THEN 'FLYWAY_HISTORY'
                             WHEN query ~* '^\\s*(COMMIT|ROLLBACK)' THEN 'TRANSACTION_END'
                             WHEN query ~* '^\\s*(BEGIN|START TRANSACTION)' THEN 'TRANSACTION_BEGIN'
                             WHEN query ~* '^\\s*SELECT' THEN 'SELECT_OTHER'
                             WHEN query ~* '^\\s*(CREATE|ALTER|DROP)' THEN 'DDL_OTHER'
                             ELSE 'OTHER' END AS query_class
                    FROM pg_stat_activity WHERE backend_type = 'client backend'
                    ORDER BY pid LIMIT 9
                    """)) {
                while (result.next()) {
                    count++;
                    observation.append("backendPid=").append(result.getInt(1))
                            .append(" state=").append(result.getString(2))
                            .append(" waitType=").append(result.getString(3))
                            .append(" wait=").append(result.getString(4))
                            .append(" queryClass=").append(result.getString(5)).append('\n');
                }
            }
        }
        Path directory = Path.of("target", "s1-b1-read-20261008", "startup-observation");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("child-" + pid + ".txt"), observation + "clientConnections=" + count + "\n");
        assertTrue(count <= 8, "B1 startup connection budget exceeded");
        System.out.println("B1_OBSERVATION childPid=" + pid + " clientConnections=" + count
                + " managementConnectionClosed=true");
    }

    /** Bounded failure evidence, saved before the existing IT deletes its original child log. */
    private static void preserveFailure(ProcessBuilder builder, Process process, Path log,
            long launchedAt, boolean aliveAtFailure, Integer exitAtFailure) throws IOException {
        final int limit = 65536;
        long size = Files.exists(log) ? Files.size(log) : 0;
        String contents = "";
        if (size > 0) {
            try (var input = Files.newInputStream(log)) {
                if (size <= limit) {
                    contents = new String(input.readNBytes(limit), java.nio.charset.StandardCharsets.UTF_8);
                } else {
                    int prefix = limit / 4;
                    byte[] beginning = input.readNBytes(prefix);
                    input.skipNBytes(size - limit);
                    contents = new String(beginning, java.nio.charset.StandardCharsets.UTF_8)
                            + "\n[middle log omitted]\n"
                            + new String(input.readNBytes(limit - prefix), java.nio.charset.StandardCharsets.UTF_8);
                }
            }
        }
        for (String name : new String[] { "SPRING_DATASOURCE_URL", "SPRING_DATASOURCE_USERNAME",
                "SPRING_DATASOURCE_PASSWORD" }) {
            String value = builder.environment().get(name);
            if (value != null && !value.isEmpty()) contents = contents.replace(value, "[REDACTED]");
        }
        contents = contents.replaceAll("(?im)^.*(?:jdbc:|password|username|credential|driverProperties).*$",
                "[connection details omitted]");
        // Redaction may grow text; bound the resulting artifact as well as the input read.
        if (contents.length() > limit) contents = contents.substring(contents.length() - limit);
        Path directory = Path.of("target", "s1-b1-read-20261008", "diagnostic-preservation");
        Files.createDirectories(directory);
        String state = "childPid=" + process.pid() + " aliveAtFailure=" + aliveAtFailure
                + " exitAtFailure=" + exitAtFailure + " exitAfterCleanup=" + process.exitValue()
                + " elapsedMs=" + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - launchedAt)
                + " originalLogBytes=" + size + " tailLimitBytes=" + limit + "\n";
        Files.writeString(directory.resolve("child-" + process.pid() + ".log"), state + contents);
        System.out.println("B1_DIAGNOSTIC childPid=" + process.pid() + " aliveAtFailure=" + aliveAtFailure
                + " exitAtFailure=" + exitAtFailure + " cleanupComplete=true");
    }
}
