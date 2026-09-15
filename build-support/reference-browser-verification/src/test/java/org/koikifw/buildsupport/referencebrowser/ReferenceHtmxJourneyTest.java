package org.koikifw.buildsupport.referencebrowser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                    () -> page.locator("#search").fill("P3B3_NO_MATCH_A"));

            assertEquals(200, searchResponse.status());
            assertTrue(searchResponse.text().contains("id=\"department-results\""));
            assertFalse(searchResponse.text().contains("<html"));
            page.waitForCondition(() -> page.url().contains("search=P3B3_NO_MATCH_A")
                    && ((Number) page.evaluate("window.__koikiAfterSwap")).intValue() > 0);
            assertTrue(page.url().contains("search=P3B3_NO_MATCH_A"));
            assertTrue(((Number) page.evaluate("window.__koikiAfterSwap")).intValue() > 0);
            assertTrue((Boolean) page.evaluate("window.__koikiBusyObserved"));
            assertNull(page.locator("#department-query").getAttribute("aria-busy"));
            page.waitForCondition(
                    () -> ((Number) page.evaluate("window.__koikiAfterSettle")).intValue() > 0);

            page.waitForResponse(
                    response -> response.url().contains("/master/departments")
                            && "GET".equals(response.request().method())
                            && "true".equalsIgnoreCase(response.request().headerValue("HX-Request")),
                    () -> page.locator("#search").fill("P3B3_NO_MATCH_B"));
            page.waitForCondition(() -> page.url().contains("search=P3B3_NO_MATCH_B"));
            assertTrue(page.url().contains("search=P3B3_NO_MATCH_B"));
            page.goBack();
            page.waitForCondition(() -> page.url().contains("search=P3B3_NO_MATCH_A"));
            assertEquals("P3B3_NO_MATCH_A", page.locator("#search").inputValue());

            page.locator("#department-create form").evaluate("form => form.noValidate = true");
            page.locator("#department-create input[name='code']").fill("invalid code");
            page.locator("#department-create input[name='name']").fill("");
            Response validationResponse = page.waitForResponse(
                    response -> response.url().endsWith("/master/departments")
                            && "POST".equals(response.request().method()),
                    () -> page.locator("#department-create button[type='submit']").click());

            assertEquals(200, validationResponse.status());
            assertEquals("true", validationResponse.request().headerValue("HX-Request"));
            assertTrue(validationResponse.request().headers().keySet().stream()
                    .map(String::toLowerCase)
                    .anyMatch(name -> name.startsWith("x-csrf")));
            page.waitForCondition(() -> page.locator("#department-create .error").count() >= 2);
            assertTrue(page.locator("#department-create .error").count() >= 2);

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

            second.locator("a:has-text('最新の申請内容を確認する')").click();
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
