package org.koikifw.buildsupport.vt;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Owns one bounded disposable DB and application at a time, including failed startup cleanup. */
final class VtReferenceFixture implements AutoCloseable {
    static final JsonMapper JSON = JsonMapper.builder().build();
    static final String USER_A = "48000000-0000-4000-8000-000000000001";
    static final String USER_B = "48000000-0000-4000-8000-000000000002";
    static final String DEPARTMENT = "48000000-0000-4000-8000-000000000011";
    static final String CATEGORY = "48000000-0000-4000-8000-000000000012";
    static final String EMAIL_A = "d2-a@example.test";
    static final String EMAIL_B = "d2-b@example.test";
    final Set<String> secrets = ConcurrentHashMap.newKeySet();
    final String password = "A9!" + random();
    final String hmac = Base64.getEncoder().encodeToString(new SecureRandom().generateSeed(32));
    final Path evidence;
    final Path processLog;
    final Path observations;
    final Path javaHome;
    final Path jar;
    final Path observer;
    final String jarHash;
    final String observerHash;
    final String runOwner;
    final List<Path> observationRuntime = new ArrayList<>();
    final java.util.Map<Path, String> observationHashes = new java.util.LinkedHashMap<>();
    final boolean observed;
    final boolean virtual;
    final int feature;
    final String mode;
    final int port;
    final String base;
    final PostgreSQLContainer postgres;
    Process application;
    boolean closed;

    VtReferenceFixture(String mode, boolean observed) throws Exception {
        assertEquals("true", System.getProperty("koiki.vt.verification.enabled"), "Explicit VT verification selection is required");
        assertEquals(805306368L, Runtime.getRuntime().maxMemory());
        this.mode = mode; this.observed = observed;
        virtual = mode.equals("C"); feature = mode.equals("A") ? 21 : 25;
        String javaInput = System.getenv(feature == 21 ? "JAVA21_HOME" : "JAVA25_HOME");
        assertNotNull(javaInput, "Supply both JDK homes");
        javaHome = Path.of(javaInput).toAbsolutePath();
        Path manifestPath = Path.of(System.getProperty("koiki.vt.manifest"));
        JsonNode manifest = JSON.readTree(manifestPath.toFile());
        if (manifest.has("sourceCommit")) {
            String expected = System.getProperty("koiki.vt.expected-commit", "");
            assertTrue(expected.matches("[0-9a-f]{40}"), "Supply the expected source commit");
            assertEquals(expected, manifest.path("sourceCommit").asText());
            assertEquals(21, manifest.path("buildJavaFeature").asInt());
            if ("true".equals(System.getenv("GITHUB_ACTIONS"))) {
                assertEquals(System.getenv("GITHUB_SHA"), expected);
                assertFalse(manifest.path("workingTreeDirty").asBoolean(true), "CI source must be clean");
            }
            runOwner = manifest.path("runOwner").asText();
            assertTrue(runOwner.matches("[0-9a-f]{32}"));
        } else {
            // Read-only replay of the frozen D2-02 manifests; CI must use the schema-1 source guard above.
            assertFalse("true".equals(System.getenv("GITHUB_ACTIONS")), "CI cannot use a legacy manifest");
            assertEquals("1284ba08a9e0cb44c67487dc02148e378699ceb4", manifest.path("baseCommit").asText());
            runOwner = java.util.UUID.randomUUID().toString().replace("-", "");
        }
        jar = Path.of(manifest.path("referenceJar").asText());
        observer = Path.of(manifest.path("observerJar").asText());
        jarHash = manifest.path("referenceSha256").asText();
        observerHash = manifest.path("observerSha256").asText();
        assertTrue(jarHash.matches("[0-9a-f]{64}")); assertTrue(observerHash.matches("[0-9a-f]{64}"));
        assertEquals(jarHash, hash(jar)); assertEquals(observerHash, hash(observer));
        try (var zip = new ZipFile(jar.toFile())) {
            var entries = zip.stream().map(java.util.zip.ZipEntry::getName).toList();
            assertTrue(entries.contains("org/springframework/boot/loader/launch/PropertiesLauncher.class"));
            assertFalse(entries.stream().anyMatch(n -> n.startsWith("BOOT-INF/classes/org/koikifw/buildsupport/")));
            assertFalse(entries.contains("BOOT-INF/classes/META-INF/resources/spa/index.html"));
            assertTrue(new String(zip.getInputStream(zip.getEntry("META-INF/MANIFEST.MF")).readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8).contains("Build-Jdk-Spec: 21"));
            var runtime = manifest.path("observationRuntime");
            assertTrue(runtime.isArray() && !runtime.isEmpty(), "Freeze the real Observability consumer runtime in the manifest");
            var coordinates = new java.util.HashSet<String>();
            for (var library : runtime) {
                String coordinate = library.path("coordinate").asText();
                String[] gav = coordinate.split(":", -1);
                assertEquals(3, gav.length, "Expected group:artifact:version");
                assertTrue(coordinates.add(gav[0] + ":" + gav[1]), "Duplicate observation runtime coordinate");
                assertTrue(java.util.Arrays.stream(gav).noneMatch(String::isBlank), "Empty runtime coordinate component");
                Path path = Path.of(library.path("jar").asText()).toAbsolutePath();
                assertTrue(Files.isRegularFile(path), "Missing frozen runtime JAR");
                assertEquals(gav[1] + "-" + gav[2] + ".jar", path.getFileName().toString());
                String expected = library.path("sha256").asText();
                assertTrue(expected.matches("[0-9a-f]{64}")); assertEquals(expected, hash(path));
                assertFalse(entries.contains("BOOT-INF/lib/" + path.getFileName()), "Do not duplicate an embedded runtime JAR");
                observationRuntime.add(path); observationHashes.put(path, expected);
            }
            assertTrue(coordinates.contains("org.koikifw:koiki-starter-observability"));
            assertTrue(coordinates.contains("io.micrometer:context-propagation"));
            assertTrue(coordinates.contains("org.springframework.boot:spring-boot-actuator-autoconfigure"));
        }
        evidence = Path.of(System.getProperty("koiki.vt.evidence")).toAbsolutePath().resolve(mode + (observed ? "-observed" : "-native"));
        Files.createDirectory(evidence);
        processLog = evidence.resolve("application.log"); observations = evidence.resolve("observations.tsv");
        try (var socket = new ServerSocket()) {
            socket.bind(new InetSocketAddress(java.net.InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 0)); port = socket.getLocalPort();
        }
        base = "http://127.0.0.1:" + port;
        secrets.add(password); secrets.add(hmac);
        postgres = new PostgreSQLContainer("postgres:17-alpine").withPassword(random())
                .withLabel("org.koikifw.vt.owner", runOwner)
                .withCommand("postgres", "-c", "max_connections=16")
                .withCreateContainerCmdModifier(command -> java.util.Objects.requireNonNull(command.getHostConfig())
                        .withMemory(1073741824L).withNanoCPUs(1000000000L));
        try {
            postgres.start();
            var info = postgres.getContainerInfo();
            var labels = java.util.Objects.requireNonNull(info.getConfig()).getLabels();
            Files.writeString(evidence.resolve("database-ownership.json"), JSON.writeValueAsString(java.util.Map.of(
                    "owner", runOwner, "containerId", postgres.getContainerId(), "imageId", info.getImageId(),
                    "sessionId", labels.getOrDefault("org.testcontainers.sessionId", ""))) + "\n");
            assertEquals(Long.valueOf(1073741824L), postgres.getContainerInfo().getHostConfig().getMemory());
            assertEquals(Long.valueOf(1000000000L), postgres.getContainerInfo().getHostConfig().getNanoCPUs());
            assertEquals(16L, scalar("select current_setting('max_connections')::bigint"));
            start();
            try (var client = new VtSessionClient(this)) {
                long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
                boolean ready = false;
                while (System.nanoTime() < deadline && !ready) {
                    assertTrue(application.isAlive(), "Application exited during startup: " + evidence);
                    try { ready = client.get("/login").statusCode() == 200
                            && client.get("/spa-api/v1/expense-requests").statusCode() == 401; }
                    catch (java.io.IOException ignored) { /* startup only; bounded deadline */ }
                    if (!ready) Thread.sleep(100);
                }
                assertTrue(ready, "Reference readiness exceeded 60 seconds");
            }
            String startup = Files.readString(processLog);
            assertTrue(java.util.regex.Pattern.compile("maximumPoolSize\\s*\\.+4\\b").matcher(startup).find(), "Initial pool maximum was not 4");
            assertTrue(java.util.regex.Pattern.compile("minimumIdle\\s*\\.+1\\b").matcher(startup).find(), "Initial pool minimum was not 1");
            assertTrue(java.util.regex.Pattern.compile("connectionTimeout\\s*\\.+3000\\b").matcher(startup).find(), "Initial pool timeout was not 3000 ms");
            if (!Boolean.getBoolean("koiki.vt.pool-diagnostic.enabled")) seed();
        } catch (Exception | AssertionError failure) {
            try { close(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    private void start() throws Exception {
        List<String> command = new ArrayList<>(List.of(javaHome.resolve("bin/java").toString(), "-Xmx768m", "-XshowSettings:vm"));
        if (Boolean.getBoolean("koiki.vt.pool-diagnostic.enabled")) {
            Path settings = evidence.resolve("hikari-diagnostic.properties");
            Files.writeString(settings, "maximumPoolSize=4\nminimumIdle=1\nconnectionTimeout=3000\nleakDetectionThreshold=2000\n");
            command.add("-Dhikaricp.configurationFile=" + settings);
        }
        if (observed && feature == 25) {
            Path settings = evidence.resolve("recording.jfc");
            Files.writeString(settings, """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <configuration version="2.0" label="D2 bounded VT" description="Same settings in B and C" provider="KOIKI Tooling">
                      <event name="jdk.VirtualThreadStart"><setting name="enabled">true</setting></event>
                      <event name="jdk.VirtualThreadEnd"><setting name="enabled">true</setting></event>
                      <event name="jdk.VirtualThreadPinned"><setting name="enabled">true</setting><setting name="threshold">20 ms</setting><setting name="stackTrace">true</setting></event>
                      <event name="jdk.VirtualThreadSubmitFailed"><setting name="enabled">true</setting></event>
                      <event name="jdk.CPULoad"><setting name="enabled">true</setting><setting name="period">1 s</setting></event>
                      <event name="jdk.GarbageCollection"><setting name="enabled">true</setting></event>
                      <event name="jdk.InitialEnvironmentVariable"><setting name="enabled">false</setting></event>
                      <event name="jdk.InitialSystemProperty"><setting name="enabled">false</setting></event>
                      <event name="jdk.ProcessStart"><setting name="enabled">false</setting></event>
                    </configuration>
                    """);
            command.add("-XX:StartFlightRecording=filename=" + evidence.resolve("runtime.jfr")
                    + ",settings=" + settings + ",maxsize=32m,dumponexit=true");
        }
        if (observed) {
            List<String> paths = new ArrayList<>(); paths.add(observer.toString());
            observationRuntime.forEach(path -> paths.add(path.toString()));
            command.addAll(List.of("-Dloader.main=org.koikifw.reference.ReferenceApplication",
                    "-Dloader.path=" + String.join(",", paths), "-cp", jar.toString(), "org.springframework.boot.loader.launch.PropertiesLauncher"));
        }
        else command.addAll(List.of("-jar", jar.toString()));
        command.addAll(List.of("--spring.datasource.hikari.maximum-pool-size=4",
                "--spring.datasource.hikari.minimum-idle=1", "--spring.datasource.hikari.connection-timeout=3000",
                "--spring.main.keep-alive=true"));
        if (Boolean.getBoolean("koiki.vt.pool-diagnostic.enabled")) {
            command.add("--spring.datasource.hikari.leak-detection-threshold=2000");
        }
        var builder = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(processLog.toFile());
        var env = builder.environment();
        for (String key : List.of("SPRING_PROFILES_ACTIVE", "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "LOADER_PATH", "LOADER_MAIN")) {
            assertTrue(!env.containsKey(key) || env.get(key).isBlank(), "Unexpected inherited launch selector: " + key);
        }
        assertTrue(env.keySet().stream().noneMatch(key -> key.startsWith("SPRING_DATASOURCE_HIKARI_")),
                "Supply Hikari settings through canonical arguments, not ambiguous environment keys");
        env.put("SERVER_ADDRESS", "127.0.0.1"); env.put("SERVER_PORT", Integer.toString(port));
        env.put("SPRING_THREADS_VIRTUAL_ENABLED", Boolean.toString(virtual));
        env.put("SPRING_DATASOURCE_URL", postgres.getJdbcUrl()
                + (postgres.getJdbcUrl().contains("?") ? "&" : "?") + "ApplicationName=d2-app&options=" + VtSessionClient.encode("-c statement_timeout=8000 -c lock_timeout=6000"));
        env.put("SPRING_DATASOURCE_USERNAME", postgres.getUsername()); env.put("SPRING_DATASOURCE_PASSWORD", postgres.getPassword());
        secrets.add(postgres.getPassword());
        env.put("SPRING_SESSION_JDBC_INITIALIZE_SCHEMA", "never"); env.put("SERVER_SERVLET_SESSION_COOKIE_SECURE", "false");
        env.put("KOIKI_REFERENCE_SOURCE_HMAC_KEY_ID", "d2-owned"); env.put("KOIKI_REFERENCE_SOURCE_HMAC_KEY", hmac);
        env.put("KOIKI_REFERENCE_NOTIFICATION_ASYNC_ENABLED", "false");
        env.put("KOIKI_REFERENCE_EXTERNAL_CHECK_ENABLED", "false");
        env.put("DEBUG", "false"); env.put("LOGGING_LEVEL_COM_ZAXXER_HIKARI", "DEBUG");
        if (observed) {
            env.put("KOIKI_VT_OBSERVE_ENABLED", "true"); env.put("KOIKI_VT_OBSERVE_OUTPUT", observations.toString());
            env.put("MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_EXCLUDE", "*");
        } else env.put("KOIKI_VT_OBSERVE_ENABLED", "false");
        application = builder.start();
        Files.writeString(evidence.resolve("ownership.txt"), "mode=" + mode + "\nobserved=" + observed + "\npid=" + application.pid()
                + "\nowner=" + runOwner + "\nprocessStartMillis=" + application.info().startInstant().orElseThrow().toEpochMilli()
                + "\ncontainer=" + postgres.getContainerId() + "\nport=" + port + "\nreferenceSha256=" + jarHash
                + "\nobserverSha256=" + observerHash + "\n");
        if (observed) Files.writeString(evidence.resolve("runtime-classpath.txt"),
                String.join("\n", observationRuntime.stream().map(Path::toString).toList()) + "\n");
    }

    Connection connection() throws Exception {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    long scalar(String sql) throws Exception {
        try (var connection = connection(); var statement = connection.createStatement()) {
            statement.setQueryTimeout(10);
            try (var rows = statement.executeQuery(sql)) { assertTrue(rows.next()); return rows.getLong(1); }
        }
    }

    void seed() throws Exception {
        try (var c = connection(); var s = c.createStatement()) {
            s.setQueryTimeout(10);
            s.execute("create extension if not exists pgcrypto");
            s.execute("insert into koiki_role(role_id,role_code,version) values ('" + DEPARTMENT + "','D2_APPLICANT',0)");
            s.execute("insert into koiki_permission(permission_id,permission_code,version) values ('" + CATEGORY + "','EXPENSE:APPLY',0)");
            s.execute("insert into koiki_role_permission(role_id,permission_id) values ('" + DEPARTMENT + "','" + CATEGORY + "')");
            for (String user : List.of(USER_A, USER_B)) {
                String email = user.equals(USER_A) ? EMAIL_A : EMAIL_B;
                try (var statement = c.prepareStatement("insert into koiki_user(user_id,email,canonical_email,status,version) values (?::uuid,?,?,'ACTIVE',0)")) {
                    statement.setString(1, user); statement.setString(2, email); statement.setString(3, email); statement.executeUpdate();
                }
                try (var statement = c.prepareStatement("insert into koiki_password_credential(user_id,encoded_password,version) values (?::uuid,'{bcrypt}' || crypt(?,gen_salt('bf',10)),0)")) {
                    statement.setString(1, user); statement.setString(2, password); statement.executeUpdate();
                }
                s.execute("insert into koiki_user_role(user_id,role_id) values ('" + user + "','" + DEPARTMENT + "')");
            }
            s.execute("insert into kkref_department(department_id,department_code,department_name,active,version,created_at,updated_at) values ('"
                    + DEPARTMENT + "','D2','D2 Department',true,0,now(),now())");
            s.execute("insert into kkref_expense_category(expense_category_id,expense_category_code,expense_category_name,active,version,created_at,updated_at) values ('"
                    + CATEGORY + "','D2_TRAVEL','D2 Travel',true,0,now(),now())");
            s.execute("insert into kkref_user_department_assignment(user_id,department_id,version,created_at,updated_at) values ('"
                    + USER_A + "','" + DEPARTMENT + "',0,now(),now())");
        }
    }

    List<String[]> records() throws Exception {
        if (!Files.exists(observations)) return List.of();
        // Read only complete records while the dedicated writer is active.
        String text = Files.readString(observations);
        int last = text.lastIndexOf('\n');
        if (last < 0) return List.of();
        return text.substring(0, last).lines().map(line -> line.split("\\|", -1)).toList();
    }

    void awaitRecords(java.util.function.Predicate<List<String[]>> predicate, Duration timeout, String description) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            assertTrue(application.isAlive(), "Application stopped while observing " + description);
            var current = records(); assertFalse(current.stream().anyMatch(r -> r[0].equals("FAIL")), "Observer reported failure");
            if (predicate.test(current)) return;
            Thread.sleep(25);
        }
        fail("Observation not established: " + description);
    }

    static String random() { byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }

    static String hash(Path path) throws Exception {
        try (var input = Files.newInputStream(path)) { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.readAllBytes())); }
    }

    @Override public void close() throws Exception {
        if (closed) return;
        closed = true;
        try {
            if (application != null) {
                application.destroy();
                if (!application.waitFor(30, TimeUnit.SECONDS)) { application.destroyForcibly(); assertTrue(application.waitFor(10, TimeUnit.SECONDS)); }
                assertFalse(application.isAlive());
            }
        } finally {
            if (postgres.isRunning()) {
                try {
                    assertEquals(0L, scalar("select count(*) from pg_stat_activity where application_name='d2-app'"),
                            "Application DB connections remain after child exit");
                } finally { postgres.stop(); }
            }
        }
        assertFalse(postgres.isRunning());
        try (var socket = new ServerSocket()) { socket.setReuseAddress(true); socket.bind(new InetSocketAddress(java.net.InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), port)); }
        assertEquals(jarHash, hash(jar)); assertEquals(observerHash, hash(observer));
        for (var entry : observationHashes.entrySet()) assertEquals(entry.getValue(), hash(entry.getKey()));
        String log = Files.exists(processLog) ? Files.readString(processLog) : "";
        for (String secret : secrets) if (secret.length() > 8) assertFalse(log.contains(secret), "Sensitive fixture value found in application log");
        assertFalse(log.contains(EMAIL_A) || log.contains(EMAIL_B), "Fixture email found in application log");
        if (observed && Files.exists(observations)) {
            var rows = records();
            assertFalse(rows.stream().anyMatch(r -> r[0].equals("FAIL")), "Observation output incomplete");
            assertTrue(rows.stream().anyMatch(r -> r[0].equals("END")), "Observer did not drain and close its output");
            assertEquals(rows.stream().filter(r -> r[0].equals("H")).count(), rows.stream().filter(r -> r[0].equals("C")).count());
            assertTrue(rows.stream().filter(r -> r[0].equals("F")).allMatch(r -> r[2].equals("true")), "Context restoration failed");
        }
        Files.writeString(evidence.resolve("cleanup.txt"), "child stopped\napplication DB connections=0\ncontainer stopped\nport released\nartifact hashes unchanged\nlog secret scan passed\n");
        if (observed && feature == 25 && Files.exists(evidence.resolve("runtime.jfr"))) {
            assertTrue(Files.size(evidence.resolve("runtime.jfr")) < 32L * 1024 * 1024, "JFR recording reached its cap");
            var jfr = new ProcessBuilder(javaHome.resolve("bin/jfr").toString(), "summary", evidence.resolve("runtime.jfr").toString())
                    .redirectErrorStream(true).redirectOutput(evidence.resolve("jfr-summary.txt").toFile()).start();
            if (!jfr.waitFor(15, TimeUnit.SECONDS)) { jfr.destroyForcibly(); jfr.waitFor(5, TimeUnit.SECONDS); fail("JFR summary exceeded deadline"); }
            assertEquals(0, jfr.exitValue());
            String summary = Files.readString(evidence.resolve("jfr-summary.txt"));
            var failed = java.util.regex.Pattern.compile("(?m)^\\s*jdk\\.VirtualThreadSubmitFailed\\s+(\\d+)").matcher(summary);
            assertTrue(failed.find(), "JFR submit failure event missing"); assertEquals("0", failed.group(1));
        }
    }
}
