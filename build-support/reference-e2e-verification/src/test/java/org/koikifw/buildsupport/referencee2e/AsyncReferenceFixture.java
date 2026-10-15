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
    LocalNotificationProviderStub stub;
    final UUID applicant = UUID.randomUUID();
    final UUID secondApplicant = UUID.randomUUID();
    final UUID approver = UUID.randomUUID();
    final UUID department = UUID.randomUUID();
    final UUID category = UUID.randomUUID();
    private final String password = "R1-" + UUID.randomUUID();
    private final String hmac = Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
    private final KeyPair key;
    private HttpServer issuer;
    private final HttpClient bearer = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final HttpClient session = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .connectTimeout(Duration.ofSeconds(2)).build();
    private final Path log;
    private Process process;
    private Process recoveryProcess;
    private Path recoveryLog;
    private String base;
    private boolean expectNoProvider;

    AsyncReferenceFixture() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        key = generator.generateKeyPair();
        log = Files.createTempFile(Path.of(System.getProperty("koiki.reference.async.verification.normal-jar"))
                .getParent(), "r1-child-", ".log");
        try {
            db.start();
            stub = new LocalNotificationProviderStub(db);
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
        } catch (Exception | Error failure) {
            try { close(); } catch (Throwable cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    private String issuerUrl() {
        return "http://127.0.0.1:" + issuer.getAddress().getPort();
    }

    void launch(Path jar, boolean async, boolean foundation) throws Exception {
        launch(jar, async, foundation, stub.endpoint());
    }

    void launch(Path jar, boolean async, boolean foundation, URI providerEndpoint) throws Exception {
        assertTrue(Files.isRegularFile(jar), "Fixed Reference JAR missing");
        var arguments = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xmx768m", "-XshowSettings:vm",
                "-Dkoiki.reference.notification.async.enabled=" + async,
                "-Dkoiki.reference.notification.foundation.enabled=" + foundation,
                "-Dkoiki.reference.notification.async.provider-endpoint=" + providerEndpoint,
                "-jar", jar.toString()));
        if (foundation) {
            arguments.add("--spring.flyway.locations=classpath:db/migration/kkref,classpath:db/migration/kkref-notification");
        }
        var builder = new ProcessBuilder(arguments).redirectErrorStream(true).redirectOutput(log.toFile());
        configureEnvironment(builder.environment());
        process = builder.start();
        Files.writeString(log.resolveSibling(log.getFileName() + ".ownership"),
                "pid=" + process.pid() + "\ncreation=" + process.info().startInstant().orElseThrow()
                        + "\ncontainer=" + db.getContainerId() + "\nartifact=" + jar + "\n");
    }

    private void configureEnvironment(java.util.Map<String, String> env) {
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
    }

    Process ownedProcess() {
        return process;
    }

    Path childLogPath() {
        return log;
    }

    Process launchRecovery(Path jar, R3RecoveryTarget target, URI providerEndpoint,
            long previousPid, Instant previousStart) throws Exception {
        return launchRecovery(jar, target, providerEndpoint, previousPid, previousStart, true);
    }

    Process launchRecovery(Path jar, R3RecoveryTarget target, URI providerEndpoint,
            long previousPid, Instant previousStart, boolean requireStopped) throws Exception {
        assertTrue(Files.isRegularFile(jar), "Fixed async Reference JAR missing");
        if (requireStopped) assertFalse(process.isAlive(), "Original Reference still running");
        recoveryLog = Files.createTempFile(log.getParent(), "r3-recovery-", ".log");
        var args = new ArrayList<>(List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xmx768m", "-XshowSettings:vm",
                "-Dspring.main.web-application-type=none",
                "-Dkoiki.reference.notification.async.enabled=true",
                "-Dkoiki.reference.notification.async.provider-endpoint=" + providerEndpoint,
                "-Dkoiki.reference.notification.recovery.enabled=true",
                "-Dkoiki.reference.notification.recovery.expected-jdbc-url=" + db.getJdbcUrl(),
                "-Dkoiki.reference.notification.recovery.environment-id=" + target.environmentId(),
                "-Dkoiki.reference.notification.recovery.publication-id=" + target.publicationId(),
                "-Dkoiki.reference.notification.recovery.event-id=" + target.eventId(),
                "-Dkoiki.reference.notification.recovery.listener-id=" + target.listenerId(),
                "-Dkoiki.reference.notification.recovery.status=" + target.status(),
                "-Dkoiki.reference.notification.recovery.completion-attempts=" + target.attempts(),
                "-Dkoiki.reference.notification.recovery.notification-key=" + target.key(),
                "-Dkoiki.reference.notification.recovery.payload-sha256=" + target.payloadHash(),
                "-Dkoiki.reference.notification.recovery.previous-pid=" + previousPid,
                "-Dkoiki.reference.notification.recovery.previous-start-instant=" + previousStart,
                "-jar", jar.toString()));
        var builder = new ProcessBuilder(args).redirectErrorStream(true).redirectOutput(recoveryLog.toFile());
        configureEnvironment(builder.environment());
        recoveryProcess = builder.start();
        Files.writeString(recoveryLog.resolveSibling(recoveryLog.getFileName() + ".ownership"),
                "pid=" + recoveryProcess.pid() + "\ncreation=" + recoveryProcess.info().startInstant().orElseThrow()
                        + "\ncontainer=" + db.getContainerId() + "\nartifact=" + jar + "\n");
        return recoveryProcess;
    }

    Path recoveryLogPath() {
        return recoveryLog;
    }

    void readyAndSeed(boolean publications) throws Exception {
        Instant deadline = Instant.now().plusSeconds(120);
        Pattern port = Pattern.compile("Tomcat started on port (\\d+)");
        while (Instant.now().isBefore(deadline)) {
            if (!process.isAlive()) { throw new AssertionError("Reference startup failed; retained bounded child log"); }
            String output = childLog();
            var match = port.matcher(output);
            if (match.find()) {
                base = "http://127.0.0.1:" + match.group(1);
                assertTrue(output.contains("Max. Heap Size: 768.00M"));
                assertTrue(Pattern.compile("maximumPoolSize\\s*\\.+4").matcher(output).find());
                assertTrue(Pattern.compile("minimumIdle\\s*\\.+0").matcher(output).find());
                seed();
                stub.grantApplicationObservation(publications);
                login();
                assertResourceSample();
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
        UUID request = submittedRequest(user);
        assertEquals(302, approveRequest(request, 2).statusCode());
        return request;
    }

    UUID submittedRequest(UUID user) throws Exception {
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
        return request;
    }

    HttpResponse<String> approveRequest(UUID request, long version) throws Exception {
        return csrfPost("/expenses/approvals/" + request + "/approve", "expectedVersion=" + version);
    }

    private HttpResponse<String> csrfPost(String route, String form) throws Exception {
        var page = session.send(HttpRequest.newBuilder(URI.create(base + "/")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, page.statusCode());
        return formPost(route, form + "&_csrf=" + encode(csrf(page.body())));
    }

    void loginAs(UUID user) throws Exception {
        try (var c = stub.admin(); var s = c.prepareStatement("""
                INSERT INTO koiki_password_credential(user_id,encoded_password,version)
                VALUES (?,'{bcrypt}' || crypt(?,gen_salt('bf',10)),0)
                ON CONFLICT(user_id) DO NOTHING
                """)) {
            s.setObject(1, user); s.setString(2, password); s.executeUpdate();
        }
        var page = session.send(HttpRequest.newBuilder(URI.create(base + "/login")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, page.statusCode());
        var response = formPost("/login", "username=" + encode(user + "@example.test")
                + "&password=" + encode(password) + "&_csrf=" + encode(csrf(page.body())));
        assertEquals(302, response.statusCode());
        assertFalse(response.headers().firstValue("Location").orElse("").contains("error"));
    }

    void removeApproverScope() throws Exception {
        try (var c = stub.admin(); var s = c.prepareStatement(
                "DELETE FROM kkref_expense_approver_scope WHERE approver_user_id=?")) {
            s.setObject(1, approver); assertEquals(1, s.executeUpdate());
        }
    }

    HttpResponse<String> deactivateDepartment() throws Exception {
        UUID role = UUID.randomUUID(); UUID permission = UUID.randomUUID();
        try (var c = stub.admin(); var s = c.createStatement()) {
            s.execute("INSERT INTO koiki_role(role_id,role_code,version) VALUES ('" + role + "','R2_MASTER',0)");
            s.execute("INSERT INTO koiki_permission(permission_id,permission_code,version) VALUES ('"
                    + permission + "','MASTER:ADMIN',0)");
            s.execute("INSERT INTO koiki_role_permission(role_id,permission_id) VALUES ('" + role + "','" + permission + "')");
            s.execute("INSERT INTO koiki_user_role(user_id,role_id) VALUES ('" + approver + "','" + role + "')");
        }
        loginAs(approver);
        return csrfPost("/master/departments/" + department + "/deactivate", "expectedVersion=0");
    }

    void assertDepartmentUnchanged() throws Exception {
        try (var c = stub.observer(); var s = c.prepareStatement(
                "SELECT active,version FROM kkref_department WHERE department_id=?")) {
            s.setObject(1, department);
            try (var rows = s.executeQuery()) {
                assertTrue(rows.next()); assertTrue(rows.getBoolean(1)); assertEquals(0, rows.getLong(2));
            }
        }
        assertEquals(0, successfulAuditCount(department, "DEACTIVATE_DEPARTMENT"));
    }

    long successfulAuditCount(UUID request, String action) throws Exception {
        try (var c = stub.observer(); var s = c.prepareStatement("""
                SELECT count(*) FROM koiki_audit_event
                WHERE audit_type='BUSINESS' AND resource_id=? AND action=? AND result='SUCCESS'
                """)) {
            s.setString(1, request.toString()); s.setString(2, action);
            try (var rows = s.executeQuery()) { rows.next(); return rows.getLong(1); }
        }
    }

    void assertSubmittedWithoutNotification(UUID request) throws Exception {
        assertState(request, "SUBMITTED", 2);
        assertEquals(0, successfulAuditCount(request, "APPROVE_EXPENSE"));
        Instant end = Instant.now().plusSeconds(5);
        do {
            assertEquals(0, publicationCount(request));
            assertEquals(0, stub.arrivalCount()); assertEquals(0, stub.receiptCount());
            assertResourceSample(); Thread.sleep(100);
        } while (Instant.now().isBefore(end));
        expectNoProvider = true;
    }

    long publicationCount(UUID request) throws Exception {
        try (var c = stub.observer(); var s = c.prepareStatement("""
                SELECT count(*) FROM kkref_notification_async.event_publication
                WHERE event_type='org.koikifw.reference.expense.domain.event.ExpenseApprovedEvent'
                  AND serialized_event::jsonb->>'expenseRequestId'=?
                """)) {
            s.setString(1, request.toString());
            try (var rows = s.executeQuery()) { rows.next(); return rows.getLong(1); }
        }
    }

    void assertNormalDenialComparison(UUID request, HttpResponse<String> response) throws Exception {
        int status = response.statusCode();
        String expected = System.getProperty("koiki.reference.async.r2.comparison.expected-status");
        if (expected != null) { assertEquals(Integer.parseInt(expected), status); }
        assertTrue(status == 403 || status == 500, "Comparison did not reach the selected denial boundary");
        assertFalse(response.body().contains("AuthorizationDeniedException"));
        assertFalse(response.body().contains("org.springframework.security"));
        assertState(request, "SUBMITTED", 2);
        assertEquals(0, successfulAuditCount(request, "APPROVE_EXPENSE"));
        Instant end = Instant.now().plusSeconds(5);
        do {
            assertEquals(0, schemas("kkref_notification_async"));
            assertEquals(0, stub.arrivalCount()); assertEquals(0, stub.receiptCount());
            assertResourceSample(); Thread.sleep(100);
        } while (Instant.now().isBefore(end));
        expectNoProvider = true;
        String output = childLog();
        boolean globalFallback = output.contains("KoikiProblemDetailsExceptionHandler")
                && output.contains("Unexpected request processing failure");
        boolean methodDenial = output.contains("AuthorizationDeniedException: Access Denied");
        if (status == 500) {
            assertTrue(globalFallback, "Unexpected 500 without the selected global fallback");
            assertTrue(methodDenial, "Unexpected 500 without method-security denial");
            assertTrue(response.body().contains("KOIKI-INTERNAL-001"));
        }
        Files.writeString(log.getParent().resolve("normal-denial-observation.json"),
                "{\"asyncEnabled\":false,\"httpStatus\":" + status
                        + ",\"methodDenialLogged\":" + methodDenial
                        + ",\"globalFallbackLogged\":" + globalFallback
                        + ",\"state\":\"SUBMITTED\",\"version\":2,\"approvalAuditCount\":0,"
                        + "\"asyncSchemaCount\":0,\"arrivals\":0,\"receipts\":0,"
                        + "\"childLog\":\"" + log.getFileName() + "\"}");
    }

    void assertState(UUID request, String state, long version) throws Exception {
        try (var c = stub.observer(); var s = c.prepareStatement(
                "SELECT status,version FROM kkref_expense_request WHERE expense_request_id=?")) {
            s.setObject(1, request);
            try (var rows = s.executeQuery()) {
                assertTrue(rows.next()); assertEquals(state, rows.getString(1)); assertEquals(version, rows.getLong(2));
            }
        }
    }

    void assertFailedNotification(UUID request) throws Exception {
        Instant end = Instant.now().plusSeconds(30);
        boolean failed = false;
        do {
            try (var c = stub.observer(); var s = c.prepareStatement("""
                    SELECT id,listener_id,status,completion_date FROM kkref_notification_async.event_publication
                    WHERE serialized_event::jsonb->>'expenseRequestId'=?
                    """)) {
                s.setString(1, request.toString());
                try (var rows = s.executeQuery()) {
                    if (rows.next() && "FAILED".equals(rows.getString(3))) {
                        assertTrue(rows.getObject(1, UUID.class) != null);
                        assertTrue(rows.getString(2).contains("ExpenseApprovalListener"));
                        assertTrue(rows.getObject(4) == null); assertFalse(rows.next()); failed = true;
                    }
                }
            }
            if (failed) { break; }
            assertResourceSample(); Thread.sleep(100);
        } while (Instant.now().isBefore(end));
        assertTrue(failed, "Target listener failure not observed");
        assertState(request, "APPROVED", 3);
        assertEquals(1, successfulAuditCount(request, "APPROVE_EXPENSE"));
        assertEquals(1, publicationCount(request));
        assertEquals(1, stub.arrivalCount());
        assertEquals(1, stub.rejectedCount("expense-approved:v1:" + request + ":3:applicant"));
        assertEquals(0, stub.receiptCount());
        assertTrue(childLog().contains("Local notification provider did not acknowledge"));
    }

    void assertResourceSample() throws Exception {
        var host = db.getContainerInfo().getHostConfig();
        assertEquals(1073741824L, host.getMemory()); assertEquals(1000000000L, host.getNanoCPUs());
        try (var c = stub.observer(); var s = c.createStatement();
                var rows = s.executeQuery("SELECT count(*) FROM pg_stat_activity WHERE datname=current_database()")) {
            rows.next(); long clients = rows.getLong(1); assertTrue(clients <= 8, "DB client limit exceeded");
            System.out.println("R2_RESOURCE container=" + db.getContainerId() + " clients=" + clients);
        }
    }

    private String childLog() throws Exception {
        assertTrue(Files.size(log) <= 10 * 1024 * 1024, "Child log limit exceeded");
        return Files.readString(log);
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
        if (match.find()) { return match.group(1); }
        var meta = Pattern.compile("name=\"_csrf\"[^>]*content=\"([^\"]+)\"").matcher(html);
        if (meta.find()) { return meta.group(1); }
        throw new AssertionError("CSRF token absent");
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
        Throwable failure = null;
        failure = cleanup(failure, () -> {
            if (recoveryProcess != null && recoveryProcess.isAlive()) {
                recoveryProcess.destroy();
                if (!recoveryProcess.waitFor(30, TimeUnit.SECONDS)) { recoveryProcess.destroyForcibly(); }
                assertTrue(recoveryProcess.waitFor(5, TimeUnit.SECONDS));
            }
        });
        failure = cleanup(failure, () -> {
            if (process != null && process.isAlive()) {
                process.destroy();
                if (!process.waitFor(30, TimeUnit.SECONDS)) { process.destroyForcibly(); }
                assertTrue(process.waitFor(5, TimeUnit.SECONDS));
            }
        });
        failure = cleanup(failure, () -> {
            if (expectNoProvider && stub != null) {
                assertEquals(0, stub.arrivalCount()); assertEquals(0, stub.receiptCount());
            }
        });
        failure = cleanup(failure, () -> { if (issuer != null) { issuer.stop(0); } });
        failure = cleanup(failure, () -> { if (stub != null) { stub.close(); } });
        failure = cleanup(failure, bearer::close);
        failure = cleanup(failure, session::close);
        failure = cleanup(failure, () -> { db.stop(); assertFalse(db.isRunning()); });
        failure = cleanup(failure, () -> {
            String output = childLog();
            Files.writeString(log, output.replace(db.getPassword(), "[REDACTED]")
                    .replace(password, "[REDACTED]").replace(hmac, "[REDACTED]"));
            assertFalse(output.contains(password)); assertFalse(output.contains(hmac));
        });
        failure = cleanup(failure, () -> {
            if (recoveryLog != null) {
                String output = Files.readString(recoveryLog);
                assertTrue(output.length() <= 10 * 1024 * 1024, "Recovery child log limit exceeded");
                Files.writeString(recoveryLog, output.replace(db.getPassword(), "[REDACTED]")
                        .replace(password, "[REDACTED]").replace(hmac, "[REDACTED]"));
                assertFalse(output.contains(password)); assertFalse(output.contains(hmac));
            }
        });
        if (failure instanceof Exception exception) { throw exception; }
        if (failure instanceof Error error) { throw error; }
    }

    private static Throwable cleanup(Throwable first, Cleanup action) {
        try { action.run(); } catch (Throwable failure) {
            if (first == null) { return failure; }
            first.addSuppressed(failure);
        }
        return first;
    }

    @FunctionalInterface
    private interface Cleanup { void run() throws Exception; }
}
