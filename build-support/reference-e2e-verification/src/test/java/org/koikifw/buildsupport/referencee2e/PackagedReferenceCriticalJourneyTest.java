package org.koikifw.buildsupport.referencee2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.sql.DriverManager;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

class PackagedReferenceCriticalJourneyTest {

    private static final String KEY_ID = "p3-c2-e2e-fixture";
    private static final String AUDIENCE = "koiki-reference-api";
    private static final String APPLICANT_EMAIL = "p3-c2-applicant@example.test";
    private static final String APPROVER_EMAIL = "p3-c2-approver@example.test";
    private static final UUID APPLICANT_ID = id(1);
    private static final UUID APPROVER_ID = id(2);
    private static final UUID APPLICANT_ROLE_ID = id(11);
    private static final UUID APPROVER_ROLE_ID = id(12);
    private static final UUID APPLY_PERMISSION_ID = id(21);
    private static final UUID APPROVE_PERMISSION_ID = id(22);
    private static final UUID MASTER_ADMIN_PERMISSION_ID = id(23);
    private static final UUID DEPARTMENT_ID = id(31);
    private static final UUID CATEGORY_ID = id(32);
    private static final Pattern CREATED_ID = Pattern.compile(
            "\\\"expenseRequestId\\\":\\\"([0-9a-f-]{36})\\\"");
    private static final Pattern SQL_LOG = Pattern.compile(
            "(?im)^\\s*Hibernate:\\s*|\\borg\\.hibernate\\.SQL\\b|Executing (?:prepared )?SQL");

    @Test
    void crossesBearerAndSessionBoundariesAndReconcilesExternalEvidence() throws Exception {
        Path repository = Path.of(System.getProperty("user.dir")).toAbsolutePath()
                .resolve("../..")
                .normalize();
        Path jar = repository.resolve(
                "koiki-reference-app/target/koiki-reference-app-0.1.0-SNAPSHOT.jar");
        assertTrue(Files.isRegularFile(jar), "Package the Reference JAR before this verification");

        KeyPair keyPair = rsaKeyPair();
        String loginPassword = randomSecret("A9!");
        String sourceHmacKey = randomBase64(32);
        Path processLog = Files.createTempFile("koiki-p3-c2-e2e-", ".log");
        Process process = null;
        IssuerFixture issuer = null;
        int applicationPort = -1;
        int issuerPort = -1;
        var postgres = new PostgreSQLContainer("postgres:17-alpine");

        try {
            postgres.start();
            issuer = new IssuerFixture(keyPair);
            issuerPort = issuer.port();
            applicationPort = availablePort();
            process = startReferenceJar(
                    jar,
                    processLog,
                    postgres,
                    issuer.issuer(),
                    applicationPort,
                    sourceHmacKey);
            String baseUrl = "http://127.0.0.1:" + applicationPort;
            waitUntilReady(process, baseUrl);
            seed(postgres, loginPassword);

            String token = token(keyPair, issuer.issuer());
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            UUID requestId = createAndSubmit(client, baseUrl, token);
            String sessionId = approveInBrowser(baseUrl, loginPassword, requestId);

            HttpResponse<String> approved = send(
                    client,
                    "GET",
                    baseUrl + "/api/v1/expense-requests/" + requestId,
                    token,
                    null);
            assertEquals(200, approved.statusCode());
            assertTrue(approved.body().contains("\"status\":\"APPROVED\""));

            assertDatabase(postgres, requestId);
            stop(process);
            String log = Files.readString(processLog);
            assertSanitizedLog(
                    log, token, loginPassword, sessionId, sourceHmacKey, APPLICANT_EMAIL, APPROVER_EMAIL);
        } finally {
            stop(process);
            if (issuer != null) {
                issuer.close();
            }
            if (postgres.isRunning()) {
                postgres.stop();
            }
            Files.deleteIfExists(processLog);
        }

        assertTrue(process == null || !process.isAlive(), "Reference process remained alive");
        assertFalse(postgres.isRunning(), "PostgreSQL container remained alive");
        assertFalse(Files.exists(processLog), "Temporary process log remained on disk");
        assertPortAvailable(applicationPort);
        assertPortAvailable(issuerPort);
    }

    private static UUID createAndSubmit(HttpClient client, String baseUrl, String token)
            throws Exception {
        String createBody = """
                {
                  "departmentId":"%s",
                  "claimedAmount":1200,
                  "lines":[{
                    "expenseCategoryId":"%s",
                    "usageDate":"2026-09-12",
                    "description":"Critical journey train",
                    "purpose":"P3-C2 verification",
                    "amount":1200
                  }]
                }
                """.formatted(DEPARTMENT_ID, CATEGORY_ID);
        HttpResponse<String> created = send(
                client, "POST", baseUrl + "/api/v1/expense-requests", token, createBody);
        assertEquals(201, created.statusCode());
        var matcher = CREATED_ID.matcher(created.body());
        assertTrue(matcher.find(), created.body());
        UUID requestId = UUID.fromString(matcher.group(1));
        assertEquals(
                "/api/v1/expense-requests/" + requestId,
                created.headers().firstValue("Location").orElseThrow());

        HttpResponse<String> draft = send(
                client,
                "GET",
                baseUrl + "/api/v1/expense-requests/" + requestId,
                token,
                null);
        assertEquals(200, draft.statusCode());
        assertTrue(draft.body().contains("\"status\":\"DRAFT\""));

        HttpResponse<String> submitted = send(
                client,
                "POST",
                baseUrl + "/api/v1/expense-requests/" + requestId + "/submit",
                token,
                "{\"expectedVersion\":1}");
        assertEquals(204, submitted.statusCode());
        return requestId;
    }

    private static String approveInBrowser(
            String baseUrl, String loginPassword, UUID requestId) {
        try (Playwright playwright = Playwright.create();
                Browser browser = playwright.chromium().launch(
                        new BrowserType.LaunchOptions().setHeadless(true));
                BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
            page.navigate(baseUrl + "/login");
            page.locator("input[name='username']").fill(APPROVER_EMAIL);
            page.locator("input[name='password']").fill(loginPassword);
            page.locator("button[type='submit']").click();
            assertEquals(baseUrl + "/", page.url());

            String sessionId = context.cookies().stream()
                    .filter(cookie -> "SESSION".equals(cookie.name))
                    .map(cookie -> cookie.value)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Session cookie was not issued"));

            page.navigate(baseUrl + "/master/departments");
            Response search = page.waitForResponse(
                    response -> response.url().contains("/master/departments")
                            && "GET".equals(response.request().method())
                            && "true".equalsIgnoreCase(
                                    response.request().headerValue("HX-Request")),
                    () -> page.locator("#search").fill("E2E"));
            assertEquals(200, search.status());
            assertTrue(search.text().contains("id=\"department-results\""));
            assertFalse(search.text().contains("<html"));
            page.waitForCondition(() -> page.locator("#department-results")
                    .textContent()
                    .contains("E2E Department"));
            assertEquals("E2E", page.locator("#search").inputValue());
            assertTrue((Boolean) page.locator("#search")
                    .evaluate("element => document.activeElement === element"));

            String approvalUrl = baseUrl + "/expenses/approvals/" + requestId;
            page.navigate(approvalUrl);
            assertTrue(page.locator("main").textContent().contains("SUBMITTED"));
            Response approved = page.waitForResponse(
                    response -> response.url().endsWith("/approve")
                            && "POST".equals(response.request().method()),
                    () -> page.locator("form[action$='/approve'] button[type='submit']").click());
            assertEquals(302, approved.status());
            page.waitForURL(baseUrl + "/expenses/approvals");
            assertTrue(page.locator("main").textContent().contains("経費申請を承認しました"));
            return sessionId;
        }
    }

    private static Process startReferenceJar(
            Path jar,
            Path log,
            PostgreSQLContainer postgres,
            String issuer,
            int port,
            String sourceHmacKey) throws IOException {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        ProcessBuilder builder = new ProcessBuilder(java, "-jar", jar.toString());
        builder.redirectErrorStream(true).redirectOutput(log.toFile());
        var environment = builder.environment();
        environment.put("SERVER_PORT", Integer.toString(port));
        environment.put("SPRING_PROFILES_ACTIVE", "api-bearer");
        environment.put("SPRING_DATASOURCE_URL", postgres.getJdbcUrl());
        environment.put("SPRING_DATASOURCE_USERNAME", postgres.getUsername());
        environment.put("SPRING_DATASOURCE_PASSWORD", postgres.getPassword());
        environment.put("SPRING_SESSION_JDBC_INITIALIZE_SCHEMA", "never");
        environment.put("KOIKI_IDENTITY_LOCAL_AUTHENTICATION_ENABLED", "true");
        environment.put("KOIKI_REFERENCE_SOURCE_HMAC_KEY_ID", "p3-c2-e2e-source");
        environment.put("KOIKI_REFERENCE_SOURCE_HMAC_KEY", sourceHmacKey);
        environment.put("KOIKI_REFERENCE_API_ISSUER", issuer);
        environment.put("KOIKI_REFERENCE_API_AUDIENCE", AUDIENCE);
        environment.put("DEBUG", "false");
        environment.put("LOGGING_LEVEL_ORG_SPRINGFRAMEWORK", "INFO");
        return builder.start();
    }

    private static void waitUntilReady(Process process, String baseUrl) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(500))
                .build();
        Instant deadline = Instant.now().plusSeconds(30);
        while (Instant.now().isBefore(deadline)) {
            if (!process.isAlive()) {
                throw new AssertionError(
                        "Reference JAR stopped during startup with exit code "
                                + process.exitValue());
            }
            try {
                HttpResponse<String> api = client.send(
                        HttpRequest.newBuilder(URI.create(
                                        baseUrl + "/api/v1/expense-requests/" + id(99)))
                                .timeout(Duration.ofSeconds(1))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
                HttpResponse<String> login = client.send(
                        HttpRequest.newBuilder(URI.create(baseUrl + "/login"))
                                .timeout(Duration.ofSeconds(1))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
                if (api.statusCode() == 401 && login.statusCode() == 200) {
                    return;
                }
            } catch (IOException ignored) {
                // Bounded startup polling.
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Reference JAR was not ready within 30 seconds");
    }

    private static void seed(PostgreSQLContainer postgres, String loginPassword)
            throws Exception {
        try (var connection = DriverManager.getConnection(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.createStatement()) {
            statement.execute("create extension if not exists pgcrypto");
            statement.execute("""
                    insert into koiki_user(user_id, email, canonical_email, status, version)
                    values
                      ('%s', '%s', '%s', 'ACTIVE', 0),
                      ('%s', '%s', '%s', 'ACTIVE', 0)
                    """.formatted(
                    APPLICANT_ID,
                    APPLICANT_EMAIL,
                    APPLICANT_EMAIL,
                    APPROVER_ID,
                    APPROVER_EMAIL,
                    APPROVER_EMAIL));
            statement.execute("""
                    insert into koiki_password_credential(user_id, encoded_password, version)
                    values ('%s', '{bcrypt}' || crypt('%s', gen_salt('bf', 10)), 0)
                    """.formatted(APPROVER_ID, loginPassword));
            statement.execute("""
                    insert into koiki_role(role_id, role_code, version)
                    values
                      ('%s', 'P3_C2_APPLICANT', 0),
                      ('%s', 'P3_C2_APPROVER', 0)
                    """.formatted(APPLICANT_ROLE_ID, APPROVER_ROLE_ID));
            statement.execute("""
                    insert into koiki_permission(permission_id, permission_code, version)
                    values
                      ('%s', 'EXPENSE:APPLY', 0),
                      ('%s', 'EXPENSE:APPROVE', 0),
                      ('%s', 'MASTER:ADMIN', 0)
                    """.formatted(
                    APPLY_PERMISSION_ID,
                    APPROVE_PERMISSION_ID,
                    MASTER_ADMIN_PERMISSION_ID));
            statement.execute("""
                    insert into koiki_user_role(user_id, role_id)
                    values ('%s', '%s'), ('%s', '%s')
                    """.formatted(
                    APPLICANT_ID, APPLICANT_ROLE_ID, APPROVER_ID, APPROVER_ROLE_ID));
            statement.execute("""
                    insert into koiki_role_permission(role_id, permission_id)
                    values
                      ('%s', '%s'),
                      ('%s', '%s'),
                      ('%s', '%s')
                    """.formatted(
                    APPLICANT_ROLE_ID,
                    APPLY_PERMISSION_ID,
                    APPROVER_ROLE_ID,
                    APPROVE_PERMISSION_ID,
                    APPROVER_ROLE_ID,
                    MASTER_ADMIN_PERMISSION_ID));
            statement.execute("""
                    insert into kkref_department
                      (department_id, department_code, department_name, active, version, created_at, updated_at)
                    values ('%s', 'E2E', 'E2E Department', true, 0, now(), now())
                    """.formatted(DEPARTMENT_ID));
            statement.execute("""
                    insert into kkref_expense_category
                      (expense_category_id, expense_category_code, expense_category_name,
                       active, version, created_at, updated_at)
                    values ('%s', 'TRAVEL', 'Travel', true, 0, now(), now())
                    """.formatted(CATEGORY_ID));
            statement.execute("""
                    insert into kkref_user_department_assignment
                      (user_id, department_id, version, created_at, updated_at)
                    values ('%s', '%s', 0, now(), now())
                    """.formatted(APPLICANT_ID, DEPARTMENT_ID));
            statement.execute("""
                    insert into kkref_expense_approver_scope
                      (approver_user_id, department_id, created_at)
                    values ('%s', '%s', now())
                    """.formatted(APPROVER_ID, DEPARTMENT_ID));
        }
    }

    private static void assertDatabase(PostgreSQLContainer postgres, UUID requestId)
            throws Exception {
        try (var connection = DriverManager.getConnection(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("""
                    select request.status,
                           request.version,
                           request.claimed_amount,
                           count(line.expense_line_id),
                           coalesce(sum(line.amount), 0)
                    from kkref_expense_request request
                    left join kkref_expense_line line
                      on line.expense_request_id = request.expense_request_id
                    where request.expense_request_id = '%s'
                    group by request.expense_request_id
                    """.formatted(requestId))) {
                assertTrue(rows.next());
                assertEquals("APPROVED", rows.getString(1));
                assertEquals(3L, rows.getLong(2));
                assertEquals(1200L, rows.getLong(3));
                assertEquals(1L, rows.getLong(4));
                assertEquals(1200L, rows.getLong(5));
                assertFalse(rows.next());
            }

            Map<String, String> actorsByAction = new HashMap<>();
            try (var rows = statement.executeQuery("""
                    select action, actor_id, result
                    from koiki_audit_event
                    where audit_type = 'BUSINESS'
                      and event_type = 'EXPENSE_WORKFLOW'
                      and resource_id = '%s'
                    order by occurred_at
                    """.formatted(requestId))) {
                while (rows.next()) {
                    assertEquals("SUCCESS", rows.getString(3));
                    actorsByAction.put(rows.getString(1), rows.getString(2));
                }
            }
            assertEquals(2, actorsByAction.size());
            assertEquals(APPLICANT_ID.toString(), actorsByAction.get("SUBMIT_EXPENSE"));
            assertEquals(APPROVER_ID.toString(), actorsByAction.get("APPROVE_EXPENSE"));

            try (var rows = statement.executeQuery("""
                    select count(*)
                    from koiki_audit_event
                    where audit_type = 'SECURITY'
                      and event_type = 'IDENTITY_LOGIN'
                      and action = 'LOGIN'
                      and result = 'SUCCESS'
                      and actor_id = '%s'
                    """.formatted(APPROVER_ID))) {
                assertTrue(rows.next());
                assertEquals(1L, rows.getLong(1));
            }
            try (var rows = statement.executeQuery("""
                    select count(*)
                    from koiki_session
                    where principal_name = '%s'
                    """.formatted(APPROVER_ID))) {
                assertTrue(rows.next());
                assertEquals(1L, rows.getLong(1));
            }
        }
    }

    private static void assertSanitizedLog(
            String log,
            String token,
            String password,
            String sessionId,
            String sourceHmacKey,
            String... emails) {
        assertFalse(log.contains(token), "Bearer token leaked to process log");
        assertFalse(log.contains(password), "Login password leaked to process log");
        assertFalse(log.contains(sessionId), "Session cookie leaked to process log");
        assertFalse(log.contains(sourceHmacKey), "Source HMAC key leaked to process log");
        for (String email : emails) {
            assertFalse(log.contains(email), "Fixture email leaked to process log");
        }
        assertFalse(SQL_LOG.matcher(log).find(), "Application emitted an SQL statement");
        assertFalse(log.contains(" ERROR "), "Application emitted an ERROR log");
        assertFalse(log.contains("Caused by:"), "Application emitted an exception cause");
    }

    private static HttpResponse<String> send(
            HttpClient client, String method, String uri, String token, String body)
            throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(uri))
                .timeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer " + token);
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String token(KeyPair keyPair, String issuer) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("p3-c2-applicant")
                .audience(List.of(AUDIENCE))
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now.minusSeconds(1)))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .claim("scope", "expense.apply")
                .claim("token_use", "access")
                .claim("koiki_user_id", APPLICANT_ID.toString())
                .build();
        SignedJWT signed = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(), claims);
        try {
            signed.sign(new RSASSASigner((RSAPrivateKey) keyPair.getPrivate()));
            return signed.serialize();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign fixture token", exception);
        }
    }

    private static void stop(Process process) throws Exception {
        if (process == null || !process.isAlive()) {
            return;
        }
        process.destroy();
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            assertTrue(process.waitFor(5, TimeUnit.SECONDS),
                    "Reference process did not terminate");
        }
    }

    private static int availablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }

    private static void assertPortAvailable(int port) throws IOException {
        assertTrue(port > 0, "Dynamic port was not allocated");
        try (ServerSocket ignored = new ServerSocket(
                port, 1, InetAddress.getLoopbackAddress())) {
            // A successful bind proves that cleanup released the loopback port.
        }
    }

    private static KeyPair rsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create fixture key", exception);
        }
    }

    private static String randomSecret(String prefix) {
        return prefix + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(24));
    }

    private static String randomBase64(int size) {
        return Base64.getEncoder().encodeToString(randomBytes(size));
    }

    private static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    private static UUID id(long suffix) {
        return new UUID(0x3800000000004000L, 0x8000000000000000L | suffix);
    }

    private static final class IssuerFixture implements AutoCloseable {

        private final HttpServer server;
        private final String issuer;

        IssuerFixture(KeyPair keyPair) throws IOException {
            server = HttpServer.create(
                    new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            issuer = "http://127.0.0.1:" + server.getAddress().getPort();
            server.createContext("/.well-known/openid-configuration", exchange -> respond(
                    exchange,
                    "{\"issuer\":\"%s\",\"jwks_uri\":\"%s/jwks\"}"
                            .formatted(issuer, issuer)));
            server.createContext("/jwks", exchange -> respond(exchange, jwks(keyPair)));
            server.start();
        }

        String issuer() {
            return issuer;
        }

        int port() {
            return server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
        }

        private static String jwks(KeyPair keyPair) {
            RSAPublicKey key = (RSAPublicKey) keyPair.getPublic();
            return """
                    {"keys":[{"kty":"RSA","kid":"%s","use":"sig","alg":"RS256","n":"%s","e":"%s"}]}
                    """.formatted(
                    KEY_ID, base64Url(key.getModulus()), base64Url(key.getPublicExponent()));
        }

        private static String base64Url(BigInteger value) {
            byte[] bytes = value.toByteArray();
            int offset = bytes.length > 1 && bytes[0] == 0 ? 1 : 0;
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(Arrays.copyOfRange(bytes, offset, bytes.length));
        }

        private static void respond(HttpExchange exchange, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var response = exchange.getResponseBody()) {
                response.write(bytes);
            }
        }
    }
}
