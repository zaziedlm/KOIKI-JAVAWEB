package org.koikifw.buildsupport.referencee2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.koikifw.buildsupport.referencee2e.PackagedReferenceCriticalJourneyTest.*;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.RequestOptions;
import com.microsoft.playwright.options.SameSiteAttribute;
import com.nimbusds.jose.util.JSONObjectUtils;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Real packaged Session SPA: no mock API and no fixture endpoint in production. */
@EnabledIfSystemProperty(named = "koiki.reference.spa.packaged-journey.enabled", matches = "true")
class PackagedReferenceSessionSpaJourneyTest {
    private static final String APPLICANT = "p3-c2-applicant@example.test";
    private static final String APPROVER = "p3-c2-approver@example.test";
    private static final String OTHER = "b1-other@example.test";
    private static final String AMOUNT = "9007199254740993";
    private static final String DESCRIPTION = "B1 <img src=x onerror=alert(1)> train";
    private static final UUID OWNER_ID = id(1);
    private static final UUID OTHER_ID = id(3);
    private static final Path EVIDENCE = Path.of("target", "b1-session-spa");

    @Test
    @Timeout(value = 10, unit = java.util.concurrent.TimeUnit.MINUTES)
    void crossesSessionSpaMvcBoundariesAndDiscardsExpiredViews() throws Exception {
        Path repository = Path.of(System.getProperty("user.dir")).toAbsolutePath().resolve("../..").normalize();
        Path jar = repository.resolve("koiki-reference-app/target/koiki-reference-app-0.1.0-SNAPSHOT.jar");
        SessionSpaArtifact.verify(repository, jar);
        assertTrue(resourceLimitsEnabled(), "Use the bounded packaged journey resource settings");
        assertEquals(805306368L, Runtime.getRuntime().maxMemory());
        String password = randomSecret("A9!");
        String hmac = randomBase64(32);
        List<String> secrets = new ArrayList<>(List.of(password, hmac));
        Path log = Files.createTempFile("koiki-b1-session-spa-", ".log");
        Process process = null;
        IssuerFixture issuer = null;
        int port = -1;
        int issuerPort = -1;
        var postgres = new PostgreSQLContainer("postgres:17-alpine")
                .withCommand("postgres", "-c", "max_connections=16")
                .withCreateContainerCmdModifier(command -> java.util.Objects.requireNonNull(command.getHostConfig())
                        .withMemory(1073741824L).withNanoCPUs(1000000000L));
        try {
            postgres.start();
            assertEquals(Long.valueOf(1073741824L), postgres.getContainerInfo().getHostConfig().getMemory());
            assertEquals(Long.valueOf(1000000000L), postgres.getContainerInfo().getHostConfig().getNanoCPUs());
            assertEquals(16L, scalar(postgres, "select current_setting('max_connections')::bigint"));
            issuer = new IssuerFixture(rsaKeyPair());
            issuerPort = issuer.port();
            port = availablePort();
            process = startReferenceJar(jar, log, postgres, issuer.issuer(), port, hmac);
            String base = "http://127.0.0.1:" + port;
            waitUntilReady(process, base);
            String startup = Files.readString(log);
            assertTrue(startup.contains("Max. Heap Size: 768.00M"));
            assertTrue(java.util.regex.Pattern.compile("maximumPoolSize\\s*\\.+4").matcher(startup).find());
            assertTrue(java.util.regex.Pattern.compile("minimumIdle\\s*\\.+1").matcher(startup).find());
            System.out.println("B1_RESOURCE dbId=" + postgres.getContainerId() + " childPid=" + process.pid()
                    + " heap=805306368 poolMaximum=4 poolMinimumIdle=1 maxConnections=16");
            seed(postgres, password);
            seedSessionUsers(postgres, password);
            try (Playwright playwright = Playwright.create();
                    Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
                    BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 800))) {
                Page page = context.newPage();
                List<String> browserErrors = new ArrayList<>();
                page.onPageError(browserErrors::add);
                anonymousBoundaries(context, base);
                login(page, base, APPLICANT, password);
                rememberCookies(context, secrets);
                var session = context.cookies().stream().filter(cookie -> cookie.name.equals("SESSION")).findFirst().orElseThrow();
                assertTrue(session.httpOnly); assertFalse(session.secure);
                assertEquals("/", session.path); assertEquals(SameSiteAttribute.LAX, session.sameSite);
                Response shell = page.navigate(base + "/spa/");
                assertEquals(200, shell.status()); assertTrue(shell.headerValue("cache-control").contains("no-store"));
                waitText(page, "申請はまだありません");
                var csrf = context.cookies().stream().filter(cookie -> cookie.name.equals("XSRF-TOKEN")).findFirst().orElseThrow();
                assertFalse(csrf.httpOnly); assertFalse(csrf.secure);
                assertEquals("/", csrf.path); assertEquals(SameSiteAttribute.LAX, csrf.sameSite);
                assertFalse((Boolean) page.evaluate("document.cookie.includes('SESSION=')"));
                // MVC's HTMX records its current path in sessionStorage. SPA must neither add
                // storage entries nor persist business data or credentials.
                String storageBefore = storageSnapshot(page);
                assertEquals(0, ((Number) page.evaluate("localStorage.length")).intValue());
                assertTrue((Boolean) page.evaluate("Object.keys(sessionStorage).every(key => key === 'htmx-current-path-for-history')"));
                assertFalse(storageBefore.contains(session.value) || storageBefore.contains(csrf.value));
                UUID requestId = createInSpa(page, context, base);
                rememberCookies(context, secrets);
                assertSafeDetail(page);
                assertDto(context, base, requestId);
                page.reload(); waitText(page, DESCRIPTION); // direct deep link / reload
                assertTrue(storageBefore.equals(storageSnapshot(page)), "SPA changed browser storage");
                Files.createDirectories(EVIDENCE);
                page.screenshot(new Page.ScreenshotOptions().setPath(EVIDENCE.resolve("detail-desktop.png")).setFullPage(true));
                page.setViewportSize(390, 844);
                page.screenshot(new Page.ScreenshotOptions().setPath(EVIDENCE.resolve("detail-mobile.png")).setFullPage(true));
                page.setViewportSize(1280, 800);
                transportAndCsrfBoundaries(context, base, requestId);
                System.out.println("B1_CHECKPOINT create-precision-storage-csrf");
                page.navigate(base + "/expenses/" + requestId);
                assertTrue(page.locator("main").textContent().contains(AMOUNT));
                assertTrue(page.locator("main").textContent().contains(DESCRIPTION));
                String mvcCsrf = page.locator("meta[name='_csrf']").getAttribute("content");
                secrets.add(mvcCsrf);
                assertStatus(context, base, "/spa-api/v1/expense-requests/" + requestId + "/submit",
                        "{\"expectedVersion\":\"1\"}", mvcCsrf, 403);
                // MVC edit advances the real aggregate version while the SPA tab retains its prior view.
                Page mvc = context.newPage();
                try {
                    page.navigate(base + "/spa/#/requests/" + requestId); waitText(page, DESCRIPTION);
                    mvc.navigate(base + "/expenses/" + requestId + "/edit");
                    mvc.locator("#purpose").fill("B1 updated through MVC");
                    String invalidFields = (String) mvc.evaluate("JSON.stringify([...document.querySelector('main form').elements].filter(e => e.willValidate && !e.validity.valid).map(e => ({name:e.name,type:e.type,missing:e.validity.valueMissing,step:e.validity.stepMismatch})))");
                    assertTrue((Boolean) mvc.locator("main form").evaluate("form => form.checkValidity()"), "MVC invalid fields: " + invalidFields);
                    Response edited = mvc.waitForResponse(response -> response.request().method().equals("POST")
                            && response.url().endsWith("/edit"), () -> mvc.locator("main button[type='submit']").click());
                    assertEquals(302, edited.status(), "MVC edit must redirect after saving");
                    System.out.println("B1_CHECKPOINT mvc-edit");
                    mvc.waitForURL(base + "/expenses/" + requestId);
                    Response conflict = page.waitForResponse(response -> response.url().endsWith("/submit"),
                            () -> button(page, "この申請を提出").click());
                    assertEquals(409, conflict.status()); waitText(page, "最新の内容を確認");
                    waitText(page, "B1 updated through MVC");
                    assertEquals(0L, businessAudits(postgres, requestId));
                    AtomicInteger submits = new AtomicInteger();
                    page.onRequest(request -> { if (request.method().equals("POST") && request.url().endsWith("/submit")) submits.incrementAndGet(); });
                    String xsrf = cookie(context, "XSRF-TOKEN");
                    Response submitted = page.waitForResponse(response -> response.url().endsWith("/submit"),
                            () -> button(page, "この申請を提出").evaluate("button => { button.click(); button.click(); }"));
                    assertEquals(204, submitted.status());
                    assertTrue(xsrf.equals(submitted.request().headerValue("X-XSRF-TOKEN")), "CSRF header differs from cookie");
                    assertTrue(submitted.request().postData().contains("\"expectedVersion\":\"2\""));
                    waitText(page, "提出済み"); assertEquals(1, submits.get());
                    mvc.reload(); assertTrue(mvc.locator("main").textContent().contains("SUBMITTED"));
                } finally { mvc.close(); }
                assertDatabase(postgres, requestId);
                System.out.println("B1_CHECKPOINT conflict-refresh-submit-db-audit");
                page.locator("a[href='#/requests']").click(); waitText(page, "全 1 件");
                page.locator("tbody a").first().click(); waitText(page, DESCRIPTION);
                page.goBack(); waitText(page, "全 1 件");
                page.goForward(); waitText(page, DESCRIPTION);
                assertStatus(context, base, "/spa/unknown", null, null, 404);
                assertStatus(context, base, "/spa-api/v2/expense-requests", null, null, 404);
                assertStatus(context, base, "/spa/assets/missing.js", null, null, 404);
                assertStatus(context, base, "/spa/koiki-reference-spa-build.json", null, null, 404);
                assertStatus(context, base, "/spa-api/logout", "{}", null, 403);
                assertStatus(context, base, "/spa-api/v1/expense-requests/" + requestId, null, null, 200);
                delayedReadAndLogout(page, context, base, requestId);
                System.out.println("B1_CHECKPOINT delayed-read-logout");
                assertEquals(0L, scalar(postgres, "select count(*) from koiki_session where principal_name='" + OWNER_ID + "'"));
                assertOldSessionRejected(base, session.value);
                page.goBack();
                assertFalse(page.locator("main").textContent().contains(DESCRIPTION));
                assertEquals(0, page.locator("tbody tr").count());
                page.reload();
                assertTrue(page.url().contains("/login"), "Reload after logout must require a fresh login");
                login(page, base, OTHER, password); rememberCookies(context, secrets);
                page.navigate(base + "/spa/#/requests/" + requestId); waitText(page, "申請が見つかりません");
                assertFalse(page.locator("main").textContent().contains(DESCRIPTION));
                assertStatus(context, base, "/spa-api/v1/expense-requests/" + requestId + "/submit",
                        "{\"expectedVersion\":\"0\"}", cookie(context, "XSRF-TOKEN"), 404);
                page.navigate(base + "/spa/"); waitText(page, "申請はまだありません");
                rememberCookies(context, secrets);
                expire(postgres, OTHER_ID);
                button(page, "再読込").click(); waitText(page, "セッションが終了");
                assertFalse(page.locator("main").textContent().contains("E2E Department"));
                assertTrue(page.url().endsWith("#/requests"));
                System.out.println("B1_CHECKPOINT foreign-scope-expiry-clear");
                login(page, base, APPROVER, password); rememberCookies(context, secrets);
                page.navigate(base + "/spa/"); waitText(page, "権限がありません");
                rememberCookies(context, secrets);
                assertFalse(page.locator("main").textContent().contains(DESCRIPTION));
                assertStatus(context, base, "/spa-api/logout", "{}", cookie(context, "XSRF-TOKEN"), 204);
                login(page, base, APPLICANT, password); rememberCookies(context, secrets);
                page.navigate(base + "/spa/#/requests/" + requestId); waitText(page, DESCRIPTION);
                // Real browser navigation away clears unsaved form state before a history restore.
                page.locator("a[href='#/new']").click(); page.locator("input[name='description']").fill("B1 unsaved secret");
                page.locator("a[href='/']").first().click(); page.goBack();
                page.locator("input[name='description']").waitFor();
                assertEquals("", page.locator("input[name='description']").inputValue());
                button(page, "ログアウト").click(); waitText(page, "ログアウトしました");
                assertFalse(page.locator("main").textContent().contains("B1 unsaved secret"));
                assertEquals(0L, scalar(postgres, "select count(*) from koiki_session where principal_name='" + OWNER_ID + "'"));
                assertTrue(browserErrors.isEmpty(), "Browser emitted a JavaScript error");
                System.out.println("B1_CHECKPOINT relogin-history-input-discard");
            }
            assertDatabase(postgres, requestIdFromDatabase(postgres));
            assertEquals(2L, scalar(postgres, "select count(*) from koiki_audit_event where audit_type='SECURITY'"
                    + " and event_type='IDENTITY_LOGIN' and action='LOGIN' and result='SUCCESS' and actor_id='" + OWNER_ID + "'"));
            assertEquals(1L, scalar(postgres, "select count(*) from koiki_audit_event where audit_type='SECURITY'"
                    + " and event_type='IDENTITY_LOGIN' and action='LOGIN' and result='SUCCESS' and actor_id='" + OTHER_ID + "'"));
            stop(process);
            String output = Files.readString(log);
            for (String secret : secrets) assertFalse(output.contains(secret), "Sensitive fixture value leaked to application log");
            assertSanitizedLog(output, "never-used-bearer-token", password, secrets.get(2), hmac, APPLICANT, APPROVER, OTHER);
            Files.writeString(EVIDENCE.resolve("sanitized-process.log"), output);
            Files.writeString(EVIDENCE.resolve("checkpoints.txt"), "artifact/input/assets matched\nreal login/cookies/CSRF\nSPA create/string precision/text escaping\nMVC read/edit and SPA 409 refresh\nexplicit submit/double-click guard\nscoped read/mutation denial\nlogout/deletion/old cookie rejection\nexpired session/401/clear\nrelogin permission change\nback/forward/reload/unsaved-input discard\nDB/Audit/log reconciled\n");
        } finally {
            stop(process);
            if (issuer != null) issuer.close();
            if (postgres.isRunning()) postgres.stop();
            Files.deleteIfExists(log);
        }
        assertTrue(process == null || !process.isAlive()); assertFalse(postgres.isRunning());
        assertFalse(Files.exists(log)); assertPortAvailable(port); assertPortAvailable(issuerPort);
        Files.writeString(EVIDENCE.resolve("cleanup.txt"), "child process stopped\nDB stopped\nissuer/application ports released\ntemporary log deleted\nbrowser/context closed\n");
    }

    private static void anonymousBoundaries(BrowserContext context, String base) {
        assertStatus(context, base, "/spa-api/v1/expense-requests", null, null, 401);
        assertTrue(context.cookies().stream().noneMatch(cookie -> cookie.name.equals("SESSION")));
        APIResponse response = context.request().get(base + "/spa/", RequestOptions.create().setMaxRedirects(0));
        try {
            assertEquals(302, response.status());
            assertTrue(response.headers().get("location").contains("/login"));
        } finally { response.dispose(); }
        assertStatus(context, base, "/spa/assets/missing.js", null, null, 404);
    }

    private static UUID createInSpa(Page page, BrowserContext context, String base) throws Exception {
        page.locator("a[href='#/new']").click();
        page.locator("input[name='amount']").fill(AMOUNT);
        page.locator("input[name='description']").fill(DESCRIPTION);
        page.locator("textarea[name='purpose']").fill("B1 Session SPA verification");
        String xsrf = cookie(context, "XSRF-TOKEN");
        Response created = page.waitForResponse(response -> response.url().equals(base + "/spa-api/v1/expense-requests")
                && response.request().method().equals("POST"), () -> button(page, "下書きを保存").click());
        assertEquals(201, created.status());
        assertTrue(xsrf.equals(created.request().headerValue("X-XSRF-TOKEN")), "CSRF header differs from cookie");
        assertTrue(created.request().headerValue("cookie").contains("SESSION="));
        assertTrue(created.request().postData().contains("\"claimedAmount\":\"" + AMOUNT + "\""));
        UUID id = UUID.fromString(JSONObjectUtils.getString(JSONObjectUtils.parse(created.text()), "expenseRequestId"));
        assertEquals("/spa-api/v1/expense-requests/" + id, created.headerValue("location"));
        waitText(page, DESCRIPTION); return id;
    }

    private static void assertDto(BrowserContext context, String base, UUID id) throws Exception {
        APIResponse detail = context.request().get(base + "/spa-api/v1/expense-requests/" + id);
        try {
            assertEquals(200, detail.status());
            var value = JSONObjectUtils.parse(detail.text());
            assertEquals("1", JSONObjectUtils.getString(value, "version"));
            assertEquals(AMOUNT, JSONObjectUtils.getString(value, "claimedAmount"));
        } finally { detail.dispose(); }
        APIResponse list = context.request().get(base + "/spa-api/v1/expense-requests");
        try {
            assertEquals(200, list.status());
            assertEquals("1", JSONObjectUtils.getString(JSONObjectUtils.parse(list.text()), "totalElements"));
        } finally { list.dispose(); }
    }

    private static void transportAndCsrfBoundaries(BrowserContext context, String base, UUID id) {
        String path = "/spa-api/v1/expense-requests/" + id + "/submit";
        assertStatus(context, base, path, "{\"expectedVersion\":\"1\"}", null, 403);
        assertStatus(context, base, path, "{\"expectedVersion\":\"1\"}", "wrong", 403);
        assertStatus(context, base, path, "{\"expectedVersion\":1}", cookie(context, "XSRF-TOKEN"), 400);
        assertStatus(context, base, path, "{\"expectedVersion\":\"0\"}", cookie(context, "XSRF-TOKEN"), 409);
        String invalid = "{\"departmentId\":\"" + id(31) + "\",\"claimedAmount\":\"2\",\"lines\":[{\"expenseCategoryId\":\""
                + id(32) + "\",\"usageDate\":\"2026-09-12\",\"description\":\"invalid sum\",\"purpose\":\"B1\",\"amount\":\"1\"}]}";
        assertStatus(context, base, "/spa-api/v1/expense-requests", invalid, cookie(context, "XSRF-TOKEN"), 422);
    }

    private static void delayedReadAndLogout(Page page, BrowserContext context, String base, UUID id) {
        AtomicReference<Route> held = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        String endpoint = base + "/spa-api/v1/expense-requests/" + id;
        page.route(endpoint, route -> {
            APIResponse response = route.fetch();
            try { assertEquals(200, response.status()); body.set(response.text()); held.set(route); }
            finally { response.dispose(); }
        });
        button(page, "再読込").click(); page.waitForCondition(() -> held.get() != null);
        Response logout = page.waitForResponse(response -> response.url().endsWith("/spa-api/logout"),
                () -> button(page, "ログアウト").click());
        assertEquals(204, logout.status());
        String deleted = logout.headerValue("set-cookie");
        assertTrue(deleted.contains("SESSION=")); assertTrue(deleted.contains("XSRF-TOKEN="));
        assertTrue(deleted.contains("Max-Age=0")); assertTrue(deleted.contains("SameSite=Lax"));
        waitText(page, "ログアウトしました");
        assertTrue(context.cookies().stream().noneMatch(cookie -> cookie.name.equals("SESSION") || cookie.name.equals("XSRF-TOKEN")));
        held.get().fulfill(new Route.FulfillOptions().setStatus(200).setContentType("application/json").setBody(body.get()));
        page.unroute(endpoint);
        page.evaluate("() => new Promise(resolve => setTimeout(resolve, 100))");
        assertFalse(page.locator("main").textContent().contains(DESCRIPTION));
        assertEquals(0, page.locator(".detail").count());
        // Even an anonymous unsafe request is rejected by CSRF before authentication.
        assertStatus(context, base, "/spa-api/logout", "{}", null, 403);
        assertStatus(context, base, "/spa-api/v1/expense-requests", null, null, 401);
    }

    private static void login(Page page, String base, String email, String password) {
        page.navigate(base + "/login");
        page.locator("input[name='username']").fill(email);
        page.locator("input[name='password']").fill(password);
        page.locator("button[type='submit']").click();
        page.waitForURL(url -> !url.contains("/login"));
    }

    private static com.microsoft.playwright.Locator button(Page page, String name) {
        return page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
    }

    private static void waitText(Page page, String text) {
        page.waitForCondition(() -> page.locator("main").textContent().contains(text));
    }

    private static void assertSafeDetail(Page page) {
        waitText(page, DESCRIPTION);
        assertTrue(page.locator("main").textContent().contains("9,007,199,254,740,993"));
        assertEquals(0, page.locator("main img").count());
    }

    private static String storageSnapshot(Page page) {
        return (String) page.evaluate("JSON.stringify([Object.entries(localStorage).sort(), Object.entries(sessionStorage).sort()])");
    }

    private static String cookie(BrowserContext context, String name) {
        return context.cookies().stream().filter(cookie -> cookie.name.equals(name)).findFirst().orElseThrow().value;
    }

    private static void rememberCookies(BrowserContext context, List<String> secrets) {
        context.cookies().forEach(cookie -> {
            secrets.add(cookie.value);
            if (cookie.name.equals("SESSION")) {
                secrets.add(new String(java.util.Base64.getDecoder().decode(cookie.value), java.nio.charset.StandardCharsets.UTF_8));
            }
        });
    }

    private static void assertStatus(BrowserContext context, String base, String path, String body, String csrf, int expected) {
        var options = RequestOptions.create().setMaxRedirects(0);
        if (csrf != null) options.setHeader("X-XSRF-TOKEN", csrf);
        APIResponse response = body == null ? context.request().get(base + path, options)
                : context.request().post(base + path, options.setHeader("Content-Type", "application/json").setData(body));
        try {
            assertEquals(expected, response.status(), "Unexpected status for " + path);
            if (path.startsWith("/spa-api/")) assertTrue(response.headers().getOrDefault("cache-control", "").contains("no-store"));
            if (expected >= 400 && path.startsWith("/spa-api/")) {
                assertTrue(response.headers().getOrDefault("content-type", "").contains("application/problem+json"));
                assertFalse(response.text().contains("<html"));
            }
        } finally { response.dispose(); }
    }

    private static void assertOldSessionRejected(String base, String value) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base + "/spa-api/v1/expense-requests"))
                .timeout(java.time.Duration.ofSeconds(5)).header("Cookie", "SESSION=" + value).GET().build();
        assertEquals(401, HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode());
    }

    private static void seedSessionUsers(PostgreSQLContainer postgres, String password) throws Exception {
        sql(postgres, "insert into koiki_password_credential(user_id,encoded_password,version) values ('" + OWNER_ID
                + "','{bcrypt}' || crypt('" + password + "',gen_salt('bf',10)),0)");
        sql(postgres, "insert into koiki_user(user_id,email,canonical_email,status,version) values ('" + OTHER_ID
                + "','" + OTHER + "','" + OTHER + "','ACTIVE',0)");
        sql(postgres, "insert into koiki_password_credential(user_id,encoded_password,version) values ('" + OTHER_ID
                + "','{bcrypt}' || crypt('" + password + "',gen_salt('bf',10)),0)");
        sql(postgres, "insert into koiki_user_role(user_id,role_id) values ('" + OTHER_ID + "','" + id(11) + "')");
    }

    private static void expire(PostgreSQLContainer postgres, UUID user) throws Exception {
        sql(postgres, "update koiki_session set last_access_time=0, expiry_time=0 where principal_name='" + user + "'");
    }

    private static long businessAudits(PostgreSQLContainer postgres, UUID id) throws Exception {
        return scalar(postgres, "select count(*) from koiki_audit_event where audit_type='BUSINESS' and resource_id='" + id + "'");
    }

    private static void assertDatabase(PostgreSQLContainer postgres, UUID id) throws Exception {
        assertEquals(1L, scalar(postgres, "select count(*) from kkref_expense_request"));
        assertEquals(1L, scalar(postgres, "select count(*) from kkref_expense_line"));
        assertEquals(1L, scalar(postgres, "select count(*) from kkref_expense_request where expense_request_id='" + id
                + "' and status='SUBMITTED' and version=3 and claimed_amount=" + AMOUNT));
        assertEquals(Long.parseLong(AMOUNT), scalar(postgres, "select sum(amount) from kkref_expense_line"));
        assertEquals(1L, businessAudits(postgres, id));
        assertEquals(1L, scalar(postgres, "select count(*) from koiki_audit_event where audit_type='BUSINESS' and action='SUBMIT_EXPENSE'"
                + " and result='SUCCESS' and actor_id='" + OWNER_ID + "' and resource_id='" + id + "'"));
        assertEquals(0L, scalar(postgres, "select count(*) from pg_namespace where nspname='kkref_notification_async'"));
    }

    private static UUID requestIdFromDatabase(PostgreSQLContainer postgres) throws Exception {
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.createStatement(); var result = statement.executeQuery("select expense_request_id from kkref_expense_request")) {
            assertTrue(result.next()); return result.getObject(1, UUID.class);
        }
    }

    private static void sql(PostgreSQLContainer postgres, String sql) throws Exception {
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.createStatement()) { statement.setQueryTimeout(10); statement.execute(sql); }
    }

    private static long scalar(PostgreSQLContainer postgres, String sql) throws Exception {
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.createStatement()) {
            statement.setQueryTimeout(10);
            try (var result = statement.executeQuery(sql)) {
                assertTrue(result.next()); return result.getLong(1);
            }
        }
    }

}
