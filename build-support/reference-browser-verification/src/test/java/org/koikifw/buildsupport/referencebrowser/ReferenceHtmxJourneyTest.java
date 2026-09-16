package org.koikifw.buildsupport.referencebrowser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReferenceHtmxJourneyTest {

    private static final String CONFLICT_REQUEST_ID =
            "b3000000-0000-4000-8000-000000000102";

    @Test
    void exercisesSelectedMasterHtmxInteractionsInARealBrowser() {
        String baseUrl = requiredSetting(
                        "koiki.browser.base-url", "KOIKI_REFERENCE_BROWSER_BASE_URL")
                .replaceAll("/+$", "");
        String loginEmail = requiredSetting(
                "koiki.browser.login-email", "KOIKI_REFERENCE_BROWSER_LOGIN_EMAIL");
        String loginPassword = requiredSetting(
                "koiki.browser.login-password", "KOIKI_REFERENCE_BROWSER_LOGIN_PASSWORD");
        boolean headless = Boolean.parseBoolean(setting(
                "koiki.browser.headless", "KOIKI_REFERENCE_BROWSER_HEADLESS", "true"));

        try (Playwright playwright = Playwright.create();
                Browser browser = playwright.chromium().launch(
                        new BrowserType.LaunchOptions().setHeadless(headless));
                BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
            page.navigate(baseUrl + "/login");
            page.locator("input[name='username']").fill(loginEmail);
            page.locator("input[name='password']").fill(loginPassword);
            page.locator("button[type='submit']").click();
            assertEquals(baseUrl + "/", page.url(),
                    "Login did not succeed. Use the random Password printed by seed-reference-demo.ps1, "
                            + "not the PostgreSQL password.");

            page.navigate(baseUrl + "/master/departments");
            assertEquals("部門管理", page.locator("h1").textContent());
            assertNotNull(page.locator("meta[name='_csrf']").getAttribute("content"));
            assertNotNull(page.locator("meta[name='_csrf_header']").getAttribute("content"));
            tabTo(page, "#search");

            page.evaluate("""
                    window.__koikiAfterSwap = 0;
                    window.__koikiAfterSettle = 0;
                    window.__koikiBusyObserved = false;
                    document.addEventListener('htmx:beforeRequest', event => {
                      if (event.detail.target?.getAttribute('aria-busy') === 'true') {
                        window.__koikiBusyObserved = true;
                      }
                    });
                    document.addEventListener('koiki:htmx:afterSwap', () => window.__koikiAfterSwap++);
                    document.addEventListener('htmx:afterSettle', () => window.__koikiAfterSettle++);
                    """);

            Response searchResponse = page.waitForResponse(
                    response -> response.url().contains("/master/departments")
                            && "GET".equals(response.request().method())
                            && "true".equalsIgnoreCase(response.request().headerValue("HX-Request")),
                    () -> page.keyboard().type("P3B3_NO_MATCH_A"));

            assertEquals(200, searchResponse.status());
            assertTrue(searchResponse.text().contains("id=\"department-results\""));
            assertFalse(searchResponse.text().contains("<html"));
            page.waitForCondition(() -> page.url().contains("search=P3B3_NO_MATCH_A")
                    && ((Number) page.evaluate("window.__koikiAfterSwap")).intValue() > 0);
            assertTrue(page.url().contains("search=P3B3_NO_MATCH_A"));
            assertTrue(((Number) page.evaluate("window.__koikiAfterSwap")).intValue() > 0);
            assertTrue((Boolean) page.evaluate("window.__koikiBusyObserved"));
            assertNull(page.locator("#department-query").getAttribute("aria-busy"));
            assertFocused(page, "#search");
            page.waitForCondition(
                    () -> ((Number) page.evaluate("window.__koikiAfterSettle")).intValue() > 0);

            page.waitForResponse(
                    response -> response.url().contains("/master/departments")
                            && "GET".equals(response.request().method())
                            && "true".equalsIgnoreCase(response.request().headerValue("HX-Request")),
                    () -> {
                        page.keyboard().press("Control+A");
                        page.keyboard().type("P3B3_NO_MATCH_B");
                    });
            page.waitForCondition(() -> page.url().contains("search=P3B3_NO_MATCH_B"));
            assertTrue(page.url().contains("search=P3B3_NO_MATCH_B"));
            page.goBack();
            page.waitForCondition(() -> page.url().contains("search=P3B3_NO_MATCH_A"));
            assertEquals("P3B3_NO_MATCH_A", page.locator("#search").inputValue());

            page.locator("#department-create form").evaluate("form => form.noValidate = true");
            page.locator("#department-create input[name='code']").fill("invalid code");
            page.locator("#department-create input[name='name']").fill("");
            page.keyboard().press("Tab");
            assertFocused(page, "#department-create button[type='submit']");
            page.keyboard().press("Shift+Tab");
            assertFocused(page, "#department-create input[name='name']");
            page.keyboard().press("Tab");
            Response validationResponse = page.waitForResponse(
                    response -> response.url().endsWith("/master/departments")
                            && "POST".equals(response.request().method()),
                    () -> page.keyboard().press("Enter"));

            assertEquals(200, validationResponse.status());
            assertEquals("true", validationResponse.request().headerValue("HX-Request"));
            assertTrue(validationResponse.request().headers().keySet().stream()
                    .map(String::toLowerCase)
                    .anyMatch(name -> name.startsWith("x-csrf")));
            page.waitForCondition(() -> page.locator("#department-create .error").count() >= 2);
            assertTrue(page.locator("#department-create .error").count() >= 2);
            page.waitForCondition(() -> isFocused(page, "#department-create #code"));
            assertFocused(page, "#department-create #code");

            @SuppressWarnings("unchecked")
            Map<String, Object> rejected = (Map<String, Object>) page.evaluate("""
                    async () => {
                      const response = await fetch('/master/departments', {
                        method: 'POST',
                        headers: {
                          'HX-Request': 'true',
                          'Content-Type': 'application/x-www-form-urlencoded'
                        },
                        body: 'code=NO_CSRF&name=No+CSRF'
                      });
                      return { status: response.status, body: await response.text() };
                    }
                    """);
            assertEquals(403, ((Number) rejected.get("status")).intValue());
            assertTrue(((String) rejected.get("body")).contains("role=\"alert\""));
            assertFalse(((String) rejected.get("body")).contains("InvalidCsrfTokenException"));
        }
    }

    @Test
    void exposesExpenseValidationErrorsThroughDescribedControls() {
        String baseUrl = requiredSetting(
                        "koiki.browser.base-url", "KOIKI_REFERENCE_BROWSER_BASE_URL")
                .replaceAll("/+$", "");
        String loginEmail = requiredSetting(
                "koiki.browser.login-email", "KOIKI_REFERENCE_BROWSER_LOGIN_EMAIL");
        String loginPassword = requiredSetting(
                "koiki.browser.login-password", "KOIKI_REFERENCE_BROWSER_LOGIN_PASSWORD");
        boolean headless = Boolean.parseBoolean(setting(
                "koiki.browser.headless", "KOIKI_REFERENCE_BROWSER_HEADLESS", "true"));

        try (Playwright playwright = Playwright.create();
                Browser browser = playwright.chromium().launch(
                        new BrowserType.LaunchOptions().setHeadless(headless));
                BrowserContext context = browser.newContext()) {
            Page page = login(context, baseUrl, loginEmail, loginPassword);
            page.navigate(baseUrl + "/expenses/new");
            page.locator("form").evaluate("form => form.noValidate = true");
            page.locator("#claimedAmount").fill("0");
            page.locator("#usageDate")
                    .fill(java.time.LocalDate.now(java.time.ZoneOffset.UTC)
                            .plusDays(2).toString());
            page.locator("#lineAmount").fill("0");
            tabTo(page, "button[type='submit']");

            Response response = page.waitForResponse(
                    candidate -> candidate.url().endsWith("/expenses")
                            && "POST".equals(candidate.request().method()),
                    () -> page.keyboard().press("Enter"));

            assertEquals(200, response.status());
            assertEquals(1, page.getByLabel("部門").count());
            assertEquals(1, page.getByLabel("申請額").count());
            assertEquals(1, page.getByLabel("経費科目").count());
            assertEquals(1, page.getByLabel("利用日").count());
            assertEquals(1, page.getByLabel("内容").count());
            assertEquals(1, page.getByLabel("目的").count());
            assertEquals(1, page.getByLabel("明細額").count());
            assertDescribedError(page, "#departmentId", "department-error");
            assertDescribedError(page, "#claimedAmount", "claimed-amount-error");
            assertDescribedError(page, "#expenseCategoryId", "expense-category-error");
            assertDescribedError(page, "#usageDate", "usage-date-error");
            assertDescribedError(page, "#description", "description-error");
            assertDescribedError(page, "#purpose", "purpose-error");
            assertDescribedError(page, "#lineAmount", "line-amount-error");
        }
    }

    @Test
    void rejectsTheLaterExpenseDecisionFromAnIndependentBrowserContext() {
        String baseUrl = requiredSetting(
                        "koiki.browser.base-url", "KOIKI_REFERENCE_BROWSER_BASE_URL")
                .replaceAll("/+$", "");
        String loginEmail = requiredSetting(
                "koiki.browser.login-email", "KOIKI_REFERENCE_BROWSER_LOGIN_EMAIL");
        String loginPassword = requiredSetting(
                "koiki.browser.login-password", "KOIKI_REFERENCE_BROWSER_LOGIN_PASSWORD");
        boolean headless = Boolean.parseBoolean(setting(
                "koiki.browser.headless", "KOIKI_REFERENCE_BROWSER_HEADLESS", "true"));

        try (Playwright playwright = Playwright.create();
                Browser browser = playwright.chromium().launch(
                        new BrowserType.LaunchOptions().setHeadless(headless));
                BrowserContext firstContext = browser.newContext();
                BrowserContext secondContext = browser.newContext()) {
            Page first = login(firstContext, baseUrl, loginEmail, loginPassword);
            Page second = login(secondContext, baseUrl, loginEmail, loginPassword);
            String detailUrl = baseUrl + "/expenses/approvals/" + CONFLICT_REQUEST_ID;

            first.navigate(detailUrl);
            second.navigate(detailUrl);
            assertEquals("1", first.locator("input[name='expectedVersion']").first().inputValue());
            assertEquals("1", second.locator("input[name='expectedVersion']").first().inputValue());

            Response firstResponse = first.waitForResponse(
                    response -> response.url().endsWith("/approve")
                            && "POST".equals(response.request().method()),
                    () -> first.locator("form[action$='/approve'] button[type='submit']").click());
            assertEquals(302, firstResponse.status());
            first.waitForURL(baseUrl + "/expenses/approvals");

            second.locator("form[action$='/reject'] textarea[name='reason']")
                    .fill("stale browser decision");
            Response secondResponse = second.waitForResponse(
                    response -> response.url().endsWith("/reject")
                            && "POST".equals(response.request().method()),
                    () -> second.locator("form[action$='/reject'] button[type='submit']").click());
            assertEquals(409, secondResponse.status());
            assertEquals("経費申請が他の操作で更新されました", second.locator("h1").textContent());
            assertTrue(second.locator("main").textContent().contains("申請を上書きしていません"));
            assertEquals(
                    "/expenses/approvals/" + CONFLICT_REQUEST_ID,
                    second.locator("a:has-text('最新の申請内容を確認する')")
                            .getAttribute("href"));

            tabTo(second, "a:has-text('最新の申請内容を確認する')");
            second.keyboard().press("Enter");
            second.waitForURL(detailUrl);
            assertTrue(second.locator("main").textContent().contains("APPROVED"));
            assertFalse(second.locator("main").textContent().contains("stale browser decision"));
        }
    }

    private static Page login(
            BrowserContext context, String baseUrl, String email, String password) {
        Page page = context.newPage();
        page.navigate(baseUrl + "/login");
        page.locator("input[name='username']").fill(email);
        page.locator("input[name='password']").fill(password);
        page.locator("button[type='submit']").click();
        assertEquals(baseUrl + "/", page.url(),
                "Login did not succeed. Use the random Password printed by seed-reference-demo.ps1, "
                        + "not the PostgreSQL password.");
        return page;
    }

    private static void tabTo(Page page, String selector) {
        for (int attempt = 0; attempt < 30; attempt++) {
            page.keyboard().press("Tab");
            if (isFocused(page, selector)) {
                return;
            }
        }
        fail("Keyboard focus did not reach " + selector);
    }

    private static void assertFocused(Page page, String selector) {
        assertTrue(isFocused(page, selector), "Expected keyboard focus on " + selector);
    }

    private static boolean isFocused(Page page, String selector) {
        return (Boolean) page.locator(selector)
                .evaluate("element => document.activeElement === element");
    }

    private static void assertDescribedError(Page page, String controlSelector, String errorId) {
        assertEquals(errorId, page.locator(controlSelector).getAttribute("aria-describedby"));
        String error = page.locator("#" + errorId).textContent();
        assertNotNull(error);
        assertFalse(error.isBlank());
    }

    private static String requiredSetting(String propertyName, String environmentName) {
        String value = setting(propertyName, environmentName, null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Required browser verification setting is missing: " + environmentName);
        }
        return value;
    }

    private static String setting(
            String propertyName, String environmentName, String defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null) {
            return propertyValue;
        }
        String environmentValue = System.getenv(environmentName);
        return environmentValue == null ? defaultValue : environmentValue;
    }
}
