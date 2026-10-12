package org.koikifw.buildsupport.referencee2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
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
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Only test-owned setup, real HTTP auth, child process and SELECT observations. */
final class AsyncReferenceFixture implements AutoCloseable {
    final PostgreSQLContainer db = NotificationProviderContractTest.postgres();
    final LocalNotificationProviderStub stub;
    final UUID applicant = UUID.randomUUID();
    final UUID secondApplicant = UUID.randomUUID();
    final UUID approver = UUID.randomUUID();
    final UUID department = UUID.randomUUID();
    final UUID category = UUID.randomUUID();
    private final String password = "R1-" + UUID.randomUUID();
    private final String hmac = Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
    private final KeyPair key;
    private final HttpServer issuer;
    private final HttpClient bearer = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final HttpClient session = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .connectTimeout(Duration.ofSeconds(2)).build();
    private final Path log;
    private Process process;
    private String base;

    AsyncReferenceFixture() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        key = generator.generateKeyPair();
        log = Files.createTempFile(Path.of(System.getProperty("koiki.reference.async.verification.normal-jar"))
                .getParent(), "r1-child-", ".log");
        db.start();
        try {
            stub = new LocalNotificationProviderStub(db);
        } catch (Exception exception) {
            db.stop();
            throw exception;
        }
        issuer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String url = issuerUrl();
        issuer.createContext("/.well-known/openid-configuration", exchange -> {
            byte[] bytes = ("{\"issuer\":\"" + url + "\",\"jwks_uri\":\"" + url + "/jwks\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        issuer.createContext("/jwks", exchange -> {
            var publicKey = new RSAKey.Builder((RSAPublicKey) key.getPublic()).keyID("r1-fixture").build();
            byte[] bytes = ("{\"keys\":[" + publicKey.toJSONString() + "]}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        issuer.start();
    }

    private String issuerUrl() {
        return "http://127.0.0.1:" + issuer.getAddress().getPort();
    }

    void launch(Path jar, boolean async, boolean foundation) throws Exception {
        assertTrue(Files.isRegularFile(jar), "Fixed Reference JAR missing");
        var arguments = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xmx768m", "-XshowSettings:vm",
                "-Dkoiki.reference.notification.async.enabled=" + async,
                "-Dkoiki.reference.notification.foundation.enabled=" + foundation,
                "-Dkoiki.reference.notification.async.provider-endpoint=" + stub.endpoint(),
                "-jar", jar.toString()));
        if (foundation) {
            arguments.add("--spring.flyway.locations=classpath:db/migration/kkref,classpath:db/migration/kkref-notification");
        }
        var builder = new ProcessBuilder(arguments).redirectErrorStream(true).redirectOutput(log.toFile());
        var env = builder.environment();
        env.put("SERVER_PORT", "0");
        env.put("SPRING_PROFILES_ACTIVE", "api-bearer");
        env.put("SPRING_DATASOURCE_URL", db.getJdbcUrl());
        env.put("SPRING_DATASOURCE_USERNAME", db.getUsername());
        env.put("SPRING_DATASOURCE_PASSWORD", db.getPassword());
        env.put("SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE", "4");
        env.put("SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE", "0");
        env.put("LOGGING_LEVEL_COM_ZAXXER_HIKARI", "DEBUG");
        env.put("SPRING_SESSION_JDBC_INITIALIZE_SCHEMA", "never");
        env.put("KOIKI_IDENTITY_LOCAL_AUTHENTICATION_ENABLED", "true");
        env.put("KOIKI_REFERENCE_SOURCE_HMAC_KEY_ID", "r1-fixture");
        env.put("KOIKI_REFERENCE_SOURCE_HMAC_KEY", hmac);
        env.put("KOIKI_REFERENCE_API_ISSUER", issuerUrl());
        env.put("KOIKI_REFERENCE_API_AUDIENCE", "koiki-reference-api");
        env.put("DEBUG", "false");
        process = builder.start();
    }

    void readyAndSeed(boolean publications) throws Exception {
        Instant deadline = Instant.now().plusSeconds(120);
        Pattern port = Pattern.compile("Tomcat started on port (\\d+)");
        while (Instant.now().isBefore(deadline)) {
            if (!process.isAlive()) { throw new AssertionError("Reference startup failed; retained bounded child log"); }
            String output = Files.readString(log);
            var match = port.matcher(output);
            if (match.find()) {
                base = "http://127.0.0.1:" + match.group(1);
                assertTrue(output.contains("Max. Heap Size: 768.00M"));
                assertTrue(Pattern.compile("maximumPoolSize\\s*\\.+4").matcher(output).find());
                assertTrue(Pattern.compile("minimumIdle\\s*\\.+0").matcher(output).find());
                seed();
                stub.grantApplicationObservation(publications);
                login();
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Reference startup deadline");
    }

    void assertMissingStarterRejected() throws Exception {
        assertTrue(process.waitFor(30, TimeUnit.SECONDS));
        assertTrue(process.exitValue() != 0);
        assertTrue(Files.readString(log).contains("Reference async selection requires the JDBC starter artifact"));
        try (var c = stub.observer(); var s = c.createStatement();
                var rows = s.executeQuery("SELECT count(*) FROM pg_tables WHERE tablename IN ('koiki_user','kkref_flyway_history','event_publication')")) {
            rows.next();
            assertEquals(0, rows.getLong(1));
        }
        assertEquals(0, stub.receiptCount());
    }

    private void seed() throws Exception {
        UUID applyRole = UUID.randomUUID(); UUID approveRole = UUID.randomUUID();
        UUID applyPermission = UUID.randomUUID(); UUID approvePermission = UUID.randomUUID();
        try (var c = stub.admin(); var s = c.createStatement()) {
            s.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto");
            for (UUID id : List.of(applicant, secondApplicant, approver)) {
                String email = id + "@example.test";
                s.execute("INSERT INTO koiki_user(user_id,email,canonical_email,status,version) VALUES ('"
                        + id + "','" + email + "','" + email + "','ACTIVE',0)");
            }
            s.execute("INSERT INTO koiki_password_credential(user_id,encoded_password,version) VALUES ('"
                    + approver + "','{bcrypt}' || crypt('" + password + "',gen_salt('bf',10)),0)");
            s.execute("INSERT INTO koiki_role(role_id,role_code,version) VALUES ('" + applyRole
                    + "','R1_APPLICANT',0),('" + approveRole + "','R1_APPROVER',0)");
            s.execute("INSERT INTO koiki_permission(permission_id,permission_code,version) VALUES ('"
                    + applyPermission + "','EXPENSE:APPLY',0),('" + approvePermission + "','EXPENSE:APPROVE',0)");
            s.execute("INSERT INTO koiki_role_permission(role_id,permission_id) VALUES ('" + applyRole
                    + "','" + applyPermission + "'),('" + approveRole + "','" + approvePermission + "')");
            for (UUID id : List.of(applicant, secondApplicant)) {
                s.execute("INSERT INTO koiki_user_role(user_id,role_id) VALUES ('" + id + "','" + applyRole + "')");
            }
            s.execute("INSERT INTO koiki_user_role(user_id,role_id) VALUES ('" + approver + "','" + approveRole + "')");
            s.execute("INSERT INTO kkref_department(department_id,department_code,department_name,active,version,created_at,updated_at)"
                    + " VALUES ('" + department + "','R1','R1 Department',true,0,now(),now())");
            s.execute("INSERT INTO kkref_expense_category(expense_category_id,expense_category_code,expense_category_name,active,version,created_at,updated_at)"
                    + " VALUES ('" + category + "','R1_TRAVEL','R1 Travel',true,0,now(),now())");
            for (UUID id : List.of(applicant, secondApplicant)) {
                s.execute("INSERT INTO kkref_user_department_assignment(user_id,department_id,version,created_at,updated_at)"
                        + " VALUES ('" + id + "','" + department + "',0,now(),now())");
            }
            s.execute("INSERT INTO kkref_expense_approver_scope(approver_user_id,department_id,created_at)"
                    + " VALUES ('" + approver + "','" + department + "',now())");
        }
    }

    private String token(UUID user) throws Exception {
        Instant now = Instant.now();
        var claims = new JWTClaimsSet.Builder().issuer(issuerUrl()).subject("r1-" + user)
                .audience(List.of("koiki-reference-api")).issueTime(Date.from(now))
                .notBeforeTime(Date.from(now.minusSeconds(1))).expirationTime(Date.from(now.plusSeconds(300)))
                .claim("scope", "expense.apply").claim("token_use", "access")
                .claim("koiki_user_id", user.toString()).build();
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("r1-fixture").build(), claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey) key.getPrivate()));
        return jwt.serialize();
    }

    private HttpResponse<String> bearerPost(String route, String body, UUID user) throws Exception {
        return bearer.send(HttpRequest.newBuilder(URI.create(base + route)).timeout(Duration.ofSeconds(5))
                .header("Authorization", "Bearer " + token(user)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    UUID approve(UUID user) throws Exception {
        String body = "{\"departmentId\":\"" + department + "\",\"claimedAmount\":1200,\"lines\":[{"
                + "\"expenseCategoryId\":\"" + category + "\",\"usageDate\":\"2026-09-12\","
                + "\"description\":\"R1 train\",\"purpose\":\"R1 notification\",\"amount\":1200}]}";
        var created = bearerPost("/api/v1/expense-requests", body, user);
        assertEquals(201, created.statusCode());
        var match = Pattern.compile("\"expenseRequestId\":\"([0-9a-f-]{36})\"").matcher(created.body());
        assertTrue(match.find());
        UUID request = UUID.fromString(match.group(1));
        assertEquals(204, bearerPost("/api/v1/expense-requests/" + request + "/submit",
                "{\"expectedVersion\":1}", user).statusCode());
        String approval = "/expenses/approvals/" + request;
        var page = session.send(HttpRequest.newBuilder(URI.create(base + approval)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, page.statusCode());
        assertEquals(302, formPost(approval + "/approve", "expectedVersion=2&_csrf=" + encode(csrf(page.body()))).statusCode());
        return request;
    }

    private void login() throws Exception {
        var page = session.send(HttpRequest.newBuilder(URI.create(base + "/login")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, page.statusCode());
        var response = formPost("/login", "username=" + encode(approver + "@example.test")
                + "&password=" + encode(password) + "&_csrf=" + encode(csrf(page.body())));
        assertEquals(302, response.statusCode());
        assertFalse(response.headers().firstValue("Location").orElse("").contains("error"));
    }

    private HttpResponse<String> formPost(String route, String body) throws Exception {
        return session.send(HttpRequest.newBuilder(URI.create(base + route)).timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String csrf(String html) {
        var match = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        if (!match.find()) { throw new AssertionError("CSRF form token absent"); }
        return match.group(1);
    }

    void assertApproval(UUID request, UUID user, boolean publications) throws Exception {
        Instant deadline = Instant.now().plusSeconds(30);
        while (publications && Instant.now().isBefore(deadline)) {
            try (var c = stub.observer(); var s = c.prepareStatement("""
                    SELECT count(*) FROM kkref_notification_async.event_publication
                    WHERE serialized_event LIKE ? AND completion_date IS NOT NULL AND status='COMPLETED'
                    """)) {
                s.setString(1, "%" + request + "%");
                try (var rows = s.executeQuery()) { rows.next(); if (rows.getLong(1) == 1) { break; } }
            }
            Thread.sleep(100);
        }
        try (var c = stub.observer()) {
            try (var s = c.prepareStatement("SELECT status,version,applicant_user_id FROM kkref_expense_request WHERE expense_request_id=?")) {
                s.setObject(1, request);
                try (var rows = s.executeQuery()) {
                    assertTrue(rows.next()); assertEquals("APPROVED", rows.getString(1));
                    assertEquals(3, rows.getLong(2)); assertEquals(user, rows.getObject(3, UUID.class));
                }
            }
            try (var s = c.prepareStatement("SELECT count(*) FROM koiki_audit_event WHERE audit_type='BUSINESS' AND event_type='EXPENSE_WORKFLOW' AND action='APPROVE_EXPENSE' AND resource_id=? AND actor_id=? AND result='SUCCESS'")) {
                s.setString(1, request.toString()); s.setString(2, approver.toString());
                try (var rows = s.executeQuery()) { rows.next(); assertEquals(1, rows.getLong(1)); }
            }
            if (publications) {
                try (var s = c.prepareStatement("""
                        SELECT p.id,p.listener_id,p.event_type,p.serialized_event,r.event_id,r.canonical_payload_hash
                        FROM kkref_notification_async.event_publication p
                        JOIN kkref_r1_provider_stub.accepted_notifications r ON r.notification_key=?
                        WHERE p.serialized_event LIKE ? AND p.completion_date IS NOT NULL AND p.status='COMPLETED'
                        """)) {
                    s.setString(1, "expense-approved:v1:" + request + ":3:applicant"); s.setString(2, "%" + request + "%");
                    try (var rows = s.executeQuery()) {
                        assertTrue(rows.next()); assertTrue(rows.getObject(1, UUID.class) != null);
                        assertTrue(rows.getString(2).contains("ExpenseApprovalListener"));
                        assertEquals("org.koikifw.reference.expense.domain.event.ExpenseApprovedEvent", rows.getString(3));
                        assertTrue(rows.getString(4).contains(rows.getString(5)));
                        assertTrue(rows.getString(4).contains(user.toString()));
                        String canonical = "{\"schemaVersion\":1,\"expenseRequestId\":\"" + request
                                + "\",\"applicantUserId\":\"" + user + "\",\"approvalVersion\":3,\"claimedAmount\":1200}";
                        assertEquals(LocalNotificationProviderStub.sha256(canonical), rows.getString(6));
                        assertFalse(rows.next());
                    }
                }
            }
        }
    }

    long schemas(String name) throws Exception {
        try (var c = stub.observer(); var s = c.prepareStatement("SELECT count(*) FROM pg_namespace WHERE nspname=?")) {
            s.setString(1, name);
            try (var rows = s.executeQuery()) { rows.next(); return rows.getLong(1); }
        }
    }

    long migrationVersion(String version) throws Exception {
        try (var c = stub.observer(); var s = c.prepareStatement("SELECT count(*) FROM kkref_flyway_history WHERE version=? AND success")) {
            s.setString(1, version);
            try (var rows = s.executeQuery()) { rows.next(); return rows.getLong(1); }
        }
    }

    @Override
    public void close() throws Exception {
        try {
            if (process != null && process.isAlive()) {
                process.destroy();
                if (!process.waitFor(30, TimeUnit.SECONDS)) { process.destroyForcibly(); }
                assertTrue(process.waitFor(5, TimeUnit.SECONDS));
            }
        } finally {
            try { stub.close(); } finally {
                issuer.stop(0); bearer.close(); session.close(); db.stop();
                assertFalse(db.isRunning());
                String output = Files.readString(log);
                assertFalse(output.contains(password)); assertFalse(output.contains(hmac));
                String clean = output.replace(db.getPassword(), "[REDACTED]");
                Files.writeString(log, clean);
            }
        }
    }
}
