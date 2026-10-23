package org.koikifw.buildsupport.referencee2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/** Isolated UI behavior over a loopback fixture, separate from the B1-07 real-JAR journey. */
@EnabledIfSystemProperty(named = "koiki.reference.spa.frontend-fixture.enabled", matches = "true")
class ReferenceSpaFrontendTest {
    private static final String ID = "00000000-0000-4000-8000-000000000001";
    private static final String LARGE = "9007199254740993";

    @Test
    void createsAndSubmitsWithoutLosingIntegerPrecision() throws Exception {
        try (var fixture = new Fixture()) {
            Page page = fixture.page;
            fixture.ready();
            Path preview = Path.of("target", "spa-frontend");
            Files.createDirectories(preview);
            page.screenshot(new Page.ScreenshotOptions().setPath(preview.resolve("list-desktop.png")).setFullPage(true));
            page.setViewportSize(390, 844);
            page.screenshot(new Page.ScreenshotOptions().setPath(preview.resolve("list-mobile.png")).setFullPage(true));
            page.setViewportSize(1280, 720);
            page.locator("a[href='#/new']").click();
            page.locator("input[name='amount']").fill(LARGE);
            page.locator("input[name='description']").fill("Visit");
            page.locator("textarea[name='purpose']").fill("Client meeting");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("下書きを保存")).click();
            page.waitForCondition(() -> page.locator("main").textContent().contains("申請の詳細"));
            assertTrue(fixture.createBody.get().contains("\"claimedAmount\":\"" + LARGE + "\""));
            assertTrue(fixture.createBody.get().contains("\"amount\":\"" + LARGE + "\""));
            assertEquals("fixture-csrf", fixture.csrfHeader.get());
            assertTrue(page.locator("main").textContent().contains("9,007,199,254,740,993"));
            assertEquals(0, page.locator("main img").count(), "Descriptions must render as text");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("この申請を提出")).click();
            page.waitForCondition(() -> page.locator(".detail .badge").textContent().contains("提出済み"));
            assertTrue(fixture.submitBody.get().contains("\"expectedVersion\":\"" + LARGE + "\""));
            assertEquals(1, fixture.creates.get()); assertEquals(1, fixture.submits.get());
        }
    }

    @Test
    void refreshesAConflictWithoutResubmittingAndClearsExpiredState() throws Exception {
        try (var fixture = new Fixture()) {
            fixture.ready();
            fixture.page.locator("tbody a").first().click();
            fixture.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("この申請を提出")).waitFor();
            fixture.conflict.set(true);
            fixture.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("この申請を提出")).click();
            fixture.page.waitForCondition(() -> fixture.page.locator("main").textContent().contains("最新の内容"));
            assertEquals(1, fixture.submits.get());
            assertTrue(fixture.details.get() >= 2, "A conflict must reload the latest detail");
            fixture.conflict.set(false);
            fixture.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("この申請を提出")).click();
            fixture.page.waitForCondition(() -> fixture.page.locator(".detail .badge").textContent().contains("提出済み"));
            assertTrue(fixture.submitBody.get().contains("\"expectedVersion\":\"9007199254740994\""));
            fixture.expired.set(true);
            fixture.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("再読込")).click();
            fixture.page.waitForCondition(() -> fixture.page.locator("main").textContent().contains("セッションが終了"));
            assertFalse(fixture.page.locator("main").textContent().contains("Fixture Finance"));
            assertTrue(fixture.page.url().endsWith("#/requests"));
        }
    }

    @Test
    void logoutPreventsADelayedReadFromRestoringData() throws Exception {
        try (var fixture = new Fixture()) {
            fixture.ready();
            fixture.delay.set(true);
            fixture.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("再読込")).click();
            assertTrue(fixture.entered.await(5, TimeUnit.SECONDS));
            fixture.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("ログアウト").setExact(true)).click();
            fixture.page.waitForCondition(() -> fixture.page.locator("main").textContent().contains("ログアウトしました"));
            fixture.release.countDown();
            assertTrue(fixture.delayedDone.await(5, TimeUnit.SECONDS));
            assertFalse(fixture.page.locator("main").textContent().contains("Fixture Finance"));
            assertEquals(0, fixture.page.locator("tbody tr").count());
            assertEquals(1, fixture.logouts.get());
        }
    }

    @Test
    void leavingAndRestoringThePageDiscardsAndReloadsTheView() throws Exception {
        try (var fixture = new Fixture()) {
            fixture.ready();
            fixture.page.evaluate("window.dispatchEvent(new PageTransitionEvent('pagehide'))");
            assertFalse(fixture.page.locator("main").textContent().contains("Fixture Finance"));
            fixture.page.evaluate("window.dispatchEvent(new PageTransitionEvent('pageshow'))");
            fixture.ready();
            assertTrue(fixture.page.locator("main").textContent().contains("Fixture Finance"));
        }
    }

    @Test
    void anUnknownMutationOutcomeDiscardsInputAndDoesNotRetry() throws Exception {
        try (var fixture = new Fixture()) {
            fixture.ready(); fixture.failCreate.set(true);
            fixture.page.locator("a[href='#/new']").click();
            fixture.page.locator("input[name='amount']").fill("1200");
            fixture.page.locator("input[name='description']").fill("Sensitive input");
            fixture.page.locator("textarea[name='purpose']").fill("Visit");
            fixture.page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("下書きを保存")).click();
            fixture.page.waitForCondition(() -> fixture.page.locator("main").textContent().contains("操作結果を確認できません"));
            assertEquals(0, fixture.page.locator("form").count());
            assertEquals(1, fixture.creates.get());
        }
    }

    private static class Fixture implements AutoCloseable {
        final AtomicBoolean expired = new AtomicBoolean(), conflict = new AtomicBoolean(), delay = new AtomicBoolean(), failCreate = new AtomicBoolean();
        final AtomicInteger details = new AtomicInteger(), creates = new AtomicInteger(), submits = new AtomicInteger(), logouts = new AtomicInteger();
        final AtomicReference<String> createBody = new AtomicReference<>(""), submitBody = new AtomicReference<>(""), csrfHeader = new AtomicReference<>("");
        final CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1), delayedDone = new CountDownLatch(1);
        final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        final HttpServer server;
        final Playwright playwright;
        final Browser browser;
        final BrowserContext context;
        final Page page;
        volatile String state = "DRAFT";
        volatile String version = LARGE;
        final Path out = Path.of("../../koiki-reference-app/target/b1-frontend").toAbsolutePath().normalize();

        Fixture() throws IOException {
            assertTrue(Files.isRegularFile(out.resolve("index.html")), "Build the frontend before running UI fixtures");
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(executor); server.createContext("/", this::handle); server.start();
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            context = browser.newContext(); page = context.newPage();
            page.navigate("http://127.0.0.1:" + server.getAddress().getPort() + "/spa/");
        }
        void ready() { page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("自分の申請")).waitFor(); }
        void handle(HttpExchange exchange) throws IOException {
            try (exchange) {
                String path = exchange.getRequestURI().getPath();
                exchange.getResponseHeaders().set("Cache-Control", "no-store");
                if (!path.startsWith("/spa-api/")) {
                    Path file = out.resolve(path.equals("/spa/") ? "index.html" : path.replaceFirst("^/spa/", "")).normalize();
                    if (!file.startsWith(out) || !Files.isRegularFile(file)) { send(exchange, 404, "{}"); return; }
                    exchange.getResponseHeaders().set("Content-Type", file.toString().endsWith(".js") ? "text/javascript" : file.toString().endsWith(".css") ? "text/css" : "text/html; charset=UTF-8");
                    byte[] bytes = Files.readAllBytes(file); exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes); return;
                }
                if (expired.get()) { send(exchange, 401, "{\"code\":\"KOIKI-REF-SESSION-001\"}"); return; }
                if (path.equals("/spa-api/csrf")) { exchange.getResponseHeaders().add("Set-Cookie", "XSRF-TOKEN=fixture-csrf; Path=/; SameSite=Lax"); send(exchange, 204, ""); return; }
                if (path.equals("/spa-api/logout")) { logouts.incrementAndGet(); send(exchange, 204, ""); return; }
                if (exchange.getRequestMethod().equals("POST")) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    csrfHeader.set(exchange.getRequestHeaders().getFirst("X-XSRF-TOKEN"));
                    if (path.endsWith("/submit")) {
                        submits.incrementAndGet(); submitBody.set(body);
                        if (conflict.get()) { version = "9007199254740994"; send(exchange, 409, "{\"code\":\"KOIKI-REF-EXPENSE-005\"}"); return; }
                        state = "SUBMITTED"; send(exchange, 204, ""); return;
                    }
                    creates.incrementAndGet(); createBody.set(body);
                    send(exchange, failCreate.get() ? 500 : 201, "{\"expenseRequestId\":\"" + ID + "\"}"); return;
                }
                String option = "{\"id\":\"" + ID + "\",\"code\":\"FIXTURE\",\"name\":\"Fixture Finance\"}";
                if (path.endsWith("expense-options")) { send(exchange, 200, "{\"departments\":[" + option + "],\"expenseCategories\":[" + option + "]}"); return; }
                String item = "{\"expenseRequestId\":\"" + ID + "\",\"department\":" + option + ",\"claimedAmount\":\"" + LARGE + "\",\"status\":\"" + state + "\",\"version\":\"" + version + "\",\"updatedAt\":\"2026-10-21T00:00:00Z\"";
                if (path.endsWith("/expense-requests")) {
                    boolean delayed = delay.compareAndSet(true, false);
                    if (delayed) { entered.countDown(); try { release.await(10, TimeUnit.SECONDS); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); } }
                    try { send(exchange, 200, "{\"content\":[" + item + "}],\"totalElements\":\"1\",\"page\":0,\"size\":20}"); }
                    finally { if (delayed) delayedDone.countDown(); }
                } else {
                    details.incrementAndGet();
                    send(exchange, 200, item + ",\"decisionReason\":null,\"lines\":[{\"expenseLineId\":\"" + ID + "\",\"expenseCategory\":" + option + ",\"usageDate\":\"2026-10-21\",\"description\":\"<img src=x onerror=alert(1)>\",\"purpose\":\"Visit\",\"amount\":\"" + LARGE + "\"}]}");
                }
            } catch (IOException disconnected) {
                // Aborting a GET during logout deliberately closes the fixture connection.
            }
        }
        void send(HttpExchange exchange, int status, String body) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, status == 204 ? -1 : bytes.length);
            if (status != 204) exchange.getResponseBody().write(bytes);
        }
        @Override
        public void close() throws Exception {
            release.countDown(); context.close(); browser.close(); playwright.close(); server.stop(0);
            executor.shutdownNow(); assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
