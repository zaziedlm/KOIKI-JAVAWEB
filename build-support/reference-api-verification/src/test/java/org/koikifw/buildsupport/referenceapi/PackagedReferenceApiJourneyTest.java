package org.koikifw.buildsupport.referenceapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.sql.DriverManager;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

class PackagedReferenceApiJourneyTest {

    private static final String KEY_ID = "p3-c1-packaged-fixture";
    private static final String AUDIENCE = "koiki-reference-api";
    private static final UUID USER_ID = id(1);
    private static final UUID DEPARTMENT_ID = id(11);
    private static final UUID CATEGORY_ID = id(12);
    private static final UUID ROLE_ID = id(21);
    private static final UUID PERMISSION_ID = id(22);
    private static final Pattern CREATED_ID = Pattern.compile(
            "\\\"expenseRequestId\\\":\\\"([0-9a-f-]{36})\\\"");

    @Test
    void runsBearerJourneyAgainstPackagedJarAndReconcilesDatabaseAndAudit() throws Exception {
        Path repository = Path.of(System.getProperty("user.dir")).toAbsolutePath()
                .resolve("../..")
                .normalize();
        Path jar = repository.resolve(
                "koiki-reference-app/target/koiki-reference-app-0.1.0-SNAPSHOT.jar");
        assertTrue(Files.isRegularFile(jar), "Package the Reference JAR before this verification");

        KeyPair keyPair = rsaKeyPair();
        Path processLog = Files.createTempFile("koiki-p3-c1-api-", ".log");
        Process process = null;
        try (var postgres = new PostgreSQLContainer("postgres:17-alpine")) {
            postgres.start();
            try (IssuerFixture issuer = new IssuerFixture(keyPair)) {
                int port = availablePort();
                process = startReferenceJar(jar, processLog, postgres, issuer.issuer(), port);
                String baseUrl = "http://127.0.0.1:" + port;
                waitUntilReady(process, baseUrl, processLog);
                seed(postgres);

                String token = token(keyPair, issuer.issuer());
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .build();
                String createBody = """
                        {
                          "departmentId":"%s",
                          "claimedAmount":1200,
                          "lines":[{
                            "expenseCategoryId":"%s",
                            "usageDate":"2026-09-12",
                            "description":"Train",
                            "purpose":"Customer visit",
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

                HttpResponse<String> detail = send(
                        client,
                        "GET",
                        baseUrl + "/api/v1/expense-requests/" + requestId,
                        token,
                        null);
                assertEquals(200, detail.statusCode());
                assertTrue(detail.body().contains("\"status\":\"DRAFT\""));
                assertTrue(detail.body().contains("\"expenseCategoryCode\":\"TRAVEL\""));
                assertFalse(detail.body().contains("applicantEmail"));

                HttpResponse<String> submitted = send(
                        client,
                        "POST",
                        baseUrl + "/api/v1/expense-requests/" + requestId + "/submit",
                        token,
                        "{\"expectedVersion\":1}");
                assertEquals(204, submitted.statusCode());

                assertDatabase(postgres, requestId);
                String log = Files.readString(processLog);
                assertFalse(log.contains(token), "Bearer token leaked to process log");
            }
        } finally {
            if (process != null) {
                process.destroy();
                if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    process.destroyForcibly().waitFor();
                }
            }
            try {
                Files.deleteIfExists(processLog);
            } catch (IOException exception) {
                processLog.toFile().deleteOnExit();
            }
        }
    }

    private static Process startReferenceJar(
            Path jar,
            Path log,
            PostgreSQLContainer postgres,
            String issuer,
            int port) throws IOException {
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
        environment.put("KOIKI_IDENTITY_LOCAL_AUTHENTICATION_ENABLED", "false");
        environment.put("KOIKI_REFERENCE_API_ISSUER", issuer);
        environment.put("KOIKI_REFERENCE_API_AUDIENCE", AUDIENCE);
        environment.put("DEBUG", "false");
        environment.put("LOGGING_LEVEL_ORG_SPRINGFRAMEWORK", "INFO");
        return builder.start();
    }

    private static void waitUntilReady(Process process, String baseUrl, Path log)
            throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(500))
                .build();
        Instant deadline = Instant.now().plusSeconds(30);
        while (Instant.now().isBefore(deadline)) {
            if (!process.isAlive()) {
                throw new AssertionError("Reference JAR stopped during startup:\n" + Files.readString(log));
            }
            try {
                HttpResponse<String> response = client.send(
                        HttpRequest.newBuilder(URI.create(
                                        baseUrl + "/api/v1/expense-requests/" + id(99)))
                                .timeout(Duration.ofSeconds(1))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 401) {
                    return;
                }
            } catch (IOException ignored) {
                // Bounded startup polling.
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Reference JAR was not ready:\n" + Files.readString(log));
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

    private static void seed(PostgreSQLContainer postgres) throws Exception {
        try (var connection = DriverManager.getConnection(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.createStatement()) {
            statement.execute("""
                    insert into koiki_user(user_id, email, canonical_email, status, version)
                    values ('%s', 'packaged@example.test', 'packaged@example.test', 'ACTIVE', 0)
                    """.formatted(USER_ID));
            statement.execute("insert into koiki_role(role_id, role_code) values ('%s', 'EXPENSE_APPLICANT')"
                    .formatted(ROLE_ID));
            statement.execute("insert into koiki_permission(permission_id, permission_code) values ('%s', 'EXPENSE:APPLY')"
                    .formatted(PERMISSION_ID));
            statement.execute("insert into koiki_user_role(user_id, role_id) values ('%s', '%s')"
                    .formatted(USER_ID, ROLE_ID));
            statement.execute("insert into koiki_role_permission(role_id, permission_id) values ('%s', '%s')"
                    .formatted(ROLE_ID, PERMISSION_ID));
            statement.execute("""
                    insert into kkref_department
                        (department_id, department_code, department_name, active, version, created_at, updated_at)
                    values ('%s', 'FINANCE', 'Finance', true, 0, now(), now())
                    """.formatted(DEPARTMENT_ID));
            statement.execute("""
                    insert into kkref_expense_category
                        (expense_category_id, expense_category_code, expense_category_name, active, version, created_at, updated_at)
                    values ('%s', 'TRAVEL', 'Travel', true, 0, now(), now())
                    """.formatted(CATEGORY_ID));
            statement.execute("""
                    insert into kkref_user_department_assignment
                        (user_id, department_id, version, created_at, updated_at)
                    values ('%s', '%s', 0, now(), now())
                    """.formatted(USER_ID, DEPARTMENT_ID));
        }
    }

    private static void assertDatabase(PostgreSQLContainer postgres, UUID requestId)
            throws Exception {
        try (var connection = DriverManager.getConnection(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("""
                    select status, version from kkref_expense_request
                    where expense_request_id = '%s'
                    """.formatted(requestId))) {
                assertTrue(rows.next());
                assertEquals("SUBMITTED", rows.getString(1));
                assertEquals(2L, rows.getLong(2));
            }
            try (var rows = statement.executeQuery("""
                    select action from koiki_audit_event
                    where event_type = 'EXPENSE_WORKFLOW' and resource_id = '%s'
                    """.formatted(requestId))) {
                assertTrue(rows.next());
                assertEquals("SUBMIT_EXPENSE", rows.getString(1));
                assertFalse(rows.next());
            }
        }
    }

    private static String token(KeyPair keyPair, String issuer) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("packaged-fixture-subject")
                .audience(List.of(AUDIENCE))
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now.minusSeconds(1)))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .claim("scope", "expense.apply")
                .claim("token_use", "access")
                .claim("koiki_user_id", USER_ID.toString())
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

    private static int availablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
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

    private static UUID id(long suffix) {
        return new UUID(0x3700000000000000L, suffix);
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
                    .encodeToString(java.util.Arrays.copyOfRange(bytes, offset, bytes.length));
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
