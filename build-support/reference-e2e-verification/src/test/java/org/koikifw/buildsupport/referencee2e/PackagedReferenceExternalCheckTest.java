package org.koikifw.buildsupport.referencee2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.DriverManager;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Explicitly selected HTTP/Session/DB integration over one immutable JAR; no browser dependency. */
@EnabledIfSystemProperty(named = "koiki.reference.c2-journey.enabled", matches = "true")
@Timeout(90)
class PackagedReferenceExternalCheckTest {
    @Test void enabledReferenceRetriesAndPreservesItsSessionBoundary() throws Exception { run(true); }
    @Test void normalReferenceDoesNotCreateTheExternalClientOrRoute() throws Exception { run(false); }

    private static void run(boolean enabled) throws Exception {
        assertEquals(805306368L, Runtime.getRuntime().maxMemory(), "Bound the test heap to 768 MiB");
        Path repository = Path.of(System.getProperty("user.dir")).toAbsolutePath().resolve("../..").normalize();
        Path jar = repository.resolve("koiki-reference-app/target/koiki-reference-app-0.1.0-SNAPSHOT.jar");
        String expected = System.getProperty("koiki.reference.c2.jar.sha256", "");
        assertTrue(expected.matches("[0-9a-f]{64}"), "Supply the frozen JAR SHA-256");
        assertEquals(expected, sha256(jar));
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            assertTrue(zip.getEntry("BOOT-INF/classes/org/koikifw/reference/externalcheck/configuration/ExternalCheckConfiguration.class") != null);
            assertTrue(zip.getEntry("BOOT-INF/classes/org/koikifw/reference/externalcheck/ExternalRateFixture.class") == null);
            assertTrue(zip.getEntry("META-INF/resources/spa/index.html") == null, "Use the normal root artifact");
        }
        var postgres = new PostgreSQLContainer("postgres:17-alpine")
                .withCommand("postgres", "-c", "max_connections=16")
                .withCreateContainerCmdModifier(command -> java.util.Objects.requireNonNull(command.getHostConfig())
                        .withMemory(1073741824L).withNanoCPUs(1000000000L));
        var stubWorkers = Executors.newFixedThreadPool(3);
        var stub = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 2);
        AtomicInteger calls = new AtomicInteger();
        stub.setExecutor(stubWorkers);
        stub.createContext("/rates", exchange -> {
            try (exchange) {
                int attempt = calls.incrementAndGet();
                if (attempt > 64) { throw new IllegalStateException("C2 fixture limit exceeded"); }
                byte[] body = "{\"base\":\"USD\",\"quote\":\"JPY\",\"date\":\"2026-10-22\",\"rate\":\"150.12345678\"}"
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(attempt < 3 ? 503 : 200, body.length);
                exchange.getResponseBody().write(body);
            }
        });
        String password = "A9!" + randomBase64();
        String hmac = randomBase64();
        List<String> secrets = new ArrayList<>(List.of(password, hmac));
        Path log = Files.createTempFile("koiki-c2-owned-", ".log");
        Process application = null;
        int port = availablePort();
        var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        try (HttpClient client = HttpClient.newBuilder().cookieHandler(cookies)
                .connectTimeout(Duration.ofSeconds(1)).followRedirects(HttpClient.Redirect.NEVER).build()) {
            try {
                postgres.start();
                assertEquals(Long.valueOf(1073741824L), postgres.getContainerInfo().getHostConfig().getMemory());
                assertEquals(Long.valueOf(1000000000L), postgres.getContainerInfo().getHostConfig().getNanoCPUs());
                try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                        var statement = connection.createStatement(); var result = statement.executeQuery("show max_connections")) {
                    assertTrue(result.next()); assertEquals(16, result.getInt(1));
                }
                stub.start();
                String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
                var builder = new ProcessBuilder(java, "-Xmx768m", "-XshowSettings:vm", "-jar", jar.toString());
                builder.redirectErrorStream(true).redirectOutput(log.toFile());
                var env = builder.environment();
                env.put("SERVER_ADDRESS", "127.0.0.1"); env.put("SERVER_PORT", Integer.toString(port));
                env.put("SPRING_PROFILES_ACTIVE", "");
                env.put("SPRING_DATASOURCE_URL", postgres.getJdbcUrl());
                env.put("SPRING_DATASOURCE_USERNAME", postgres.getUsername());
                env.put("SPRING_DATASOURCE_PASSWORD", postgres.getPassword());
                env.put("SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE", "4");
                env.put("SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE", "1");
                env.put("SPRING_SESSION_JDBC_INITIALIZE_SCHEMA", "never");
                env.put("SERVER_SERVLET_SESSION_COOKIE_SECURE", "false");
                env.put("KOIKI_IDENTITY_LOCAL_AUTHENTICATION_ENABLED", "true");
                env.put("KOIKI_REFERENCE_SOURCE_HMAC_KEY_ID", "c2-owned");
                env.put("KOIKI_REFERENCE_SOURCE_HMAC_KEY", hmac);
                env.put("KOIKI_REFERENCE_EXTERNAL_CHECK_ENABLED", Boolean.toString(enabled));
                env.put("KOIKI_REFERENCE_EXTERNAL_CHECK_PROVIDER_BASE_URI", enabled
                        ? "http://127.0.0.1:" + stub.getAddress().getPort() : "http://invalid.example.test/disabled");
                env.put("DEBUG", "false");
                env.put("LOGGING_LEVEL_COM_ZAXXER_HIKARI", "DEBUG");
                application = builder.start();
                String base = "http://127.0.0.1:" + port;
                waitReady(application, client, base);
                String startup = Files.readString(log);
                assertTrue(startup.contains("Max. Heap Size: 768.00M"));
                assertTrue(Pattern.compile("maximumPoolSize\\s*\\.+4").matcher(startup).find());
                assertTrue(Pattern.compile("minimumIdle\\s*\\.+1").matcher(startup).find());
                PackagedReferenceCriticalJourneyTest.seed(postgres, password);
                grantExternalPermission(postgres);
                String path = base + "/spa-api/v1/external-checks/rate?base=USD&quote=JPY&date=2026-10-22";
                assertEquals(401, get(client, path).statusCode());
                String loginPage = get(client, base + "/login").body();
                var token = Pattern.compile("<input[^>]*name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(loginPage);
                assertTrue(token.find(), "Login page must provide its MVC CSRF token");
                String csrf = token.group(1); secrets.add(csrf);
                String form = "username=" + encode("p3-c2-approver@example.test") + "&password=" + encode(password)
                        + "&_csrf=" + encode(csrf);
                var login = client.send(HttpRequest.newBuilder(URI.create(base + "/login")).timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.discarding());
                assertEquals(302, login.statusCode());
                assertEquals(URI.create(base + "/"), URI.create(base).resolve(login.headers().firstValue("location").orElse("")));
                cookies.getCookieStore().getCookies().forEach(cookie -> secrets.add(cookie.getValue()));
                var response = get(client, path);
                assertEquals(enabled ? 200 : 404, response.statusCode());
                if (enabled) {
                    assertTrue(response.body().contains("\"rate\":\"150.12345678\""));
                    assertEquals(3, calls.get());
                    assertTrue(response.headers().firstValue("cache-control").orElse("").contains("no-store"));
                } else { assertEquals(0, calls.get()); }
                assertEquals(404, get(client, base + "/spa-api/v1/external-checks/receipts").statusCode());
                try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                        var statement = connection.createStatement(); var rows = statement.executeQuery("select count(*) from kkref_expense_request")) {
                    assertTrue(rows.next()); assertEquals(0, rows.getInt(1), "External read must not create expense data");
                }
                assertEquals(expected, sha256(jar));
                System.out.println("C2_PACKAGED enabled=" + enabled + " requests=" + calls.get()
                        + " jarSha256=" + expected + " childPid=" + application.pid());
            } finally {
                if (application != null) {
                    PackagedReferenceCriticalJourneyTest.stop(application);
                    assertFalse(application.isAlive());
                }
                stub.stop(0); stubWorkers.shutdownNow();
                assertTrue(stubWorkers.awaitTermination(2, TimeUnit.SECONDS));
                postgres.stop(); assertFalse(postgres.isRunning());
                try (var check = new ServerSocket()) {
                    check.setReuseAddress(true); check.bind(new InetSocketAddress("127.0.0.1", port));
                }
                String output = Files.readString(log);
                for (String secret : secrets) { assertFalse(output.contains(secret), "Sensitive fixture value leaked"); }
                assertFalse(Pattern.compile("(?im)^\\s*Hibernate:|Executing (?:prepared )?SQL").matcher(output).find());
                Path evidence = repository.resolve("build-support/reference-e2e-verification/target/c2-evidence");
                Files.createDirectories(evidence);
                Files.writeString(evidence.resolve(enabled ? "enabled-sanitized.log" : "disabled-sanitized.log"), output);
                Files.deleteIfExists(log);
            }
        }
    }

    private static void grantExternalPermission(PostgreSQLContainer postgres) throws Exception {
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.createStatement()) {
            statement.execute("insert into koiki_permission(permission_id,permission_code,version) values "
                    + "('92000000-0000-4000-8000-000000000001','EXTERNALCHECK:USE',0)");
            statement.execute("insert into koiki_role_permission(role_id,permission_id) "
                    + "select role_id,'92000000-0000-4000-8000-000000000001'::uuid from koiki_role where role_code='P3_C2_APPROVER'");
        }
    }

    private static HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static void waitReady(Process process, HttpClient client, String base) throws Exception {
        Instant deadline = Instant.now().plusSeconds(30);
        while (Instant.now().isBefore(deadline)) {
            assertTrue(process.isAlive(), "Owned Reference process stopped before startup");
            try { if (get(client, base + "/login").statusCode() == 200) { return; } }
            catch (java.io.IOException failure) { /* Bounded loopback readiness polling. */ }
            Thread.sleep(100);
        }
        throw new AssertionError("Owned Reference did not start within 30 seconds");
    }

    private static int availablePort() throws Exception {
        try (var socket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) { return socket.getLocalPort(); }
    }
    private static String randomBase64() { return Base64.getEncoder().encodeToString(new SecureRandom().generateSeed(32)); }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static String sha256(Path path) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))); }
}
