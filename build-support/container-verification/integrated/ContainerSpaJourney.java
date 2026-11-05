import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import java.nio.file.*;
import java.util.*;

/** Standalone browser runner against the already running app container. */
public class ContainerSpaJourney {
    static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
    public static void main(String[] args) throws Exception {
        String base = args[0];
        String password = Files.readString(Path.of(args[1])).strip();
        Path screenshot = Path.of(args[2]);
        List<String> errors = new ArrayList<>();
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
             BrowserContext context = browser.newContext()) {
            Page page = context.newPage();
            page.onPageError(errors::add);
            page.setDefaultTimeout(20000);
            page.navigate(base + "/login");
            page.locator("input[name='username']").fill("d3-applicant@example.test");
            page.locator("input[name='password']").fill(password);
            page.locator("button[type='submit']").click();
            page.waitForURL(base + "/");
            page.navigate(base + "/spa/");
            page.getByText("申請はまだありません。", new Page.GetByTextOptions().setExact(false)).waitFor();
            require(context.cookies().stream().anyMatch(c -> c.name.equals("SESSION") && c.httpOnly), "HttpOnly Session missing");
            require(!(Boolean)page.evaluate("document.cookie.includes('SESSION=')"), "Session exposed to JS");
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("新しい申請").setExact(true)).click();
            page.getByLabel("金額（円）").fill("1200");
            page.getByLabel("内容", new Page.GetByLabelOptions().setExact(true)).fill("D3 container travel");
            page.getByLabel("目的", new Page.GetByLabelOptions().setExact(true)).fill("D3 integrated verification");
            Response created = page.waitForResponse(r -> r.request().method().equals("POST")
                    && r.url().endsWith("/spa-api/v1/expense-requests"), () ->
                    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("下書きを保存")).click());
            require(created.status() == 201, "Create status " + created.status());
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("この申請を提出")).waitFor();
            String id = page.url().substring(page.url().lastIndexOf('/') + 1);
            require(id.matches("[a-f0-9-]{36}"), "Invalid created id");
            APIResponse rejected = context.request().post(base + "/spa-api/v1/expense-requests/" + id + "/submit",
                    RequestOptions.create().setHeader("Content-Type", "application/json").setData("{\"expectedVersion\":\"0\"}"));
            try {
                require(rejected.status() == 403, "Missing CSRF was not rejected");
            } finally { rejected.dispose(); }
            Response submitted = page.waitForResponse(r -> r.request().method().equals("POST") && r.url().endsWith("/submit"), () ->
                    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("この申請を提出")).click());
            require(submitted.status() == 204, "Submit status " + submitted.status());
            page.getByText("提出済み", new Page.GetByTextOptions().setExact(true)).waitFor();
            page.reload();
            page.getByText("D3 container travel", new Page.GetByTextOptions().setExact(true)).waitFor();
            page.screenshot(new Page.ScreenshotOptions().setPath(screenshot).setFullPage(true));
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("ログアウト").setExact(true)).click();
            page.getByText("ログアウトしました。", new Page.GetByTextOptions().setExact(true)).waitFor();
            APIResponse denied = context.request().get(base + "/spa-api/v1/expense-requests");
            try {
                require(denied.status() == 401, "Logged out Session still authorized");
            } finally { denied.dispose(); }
            require(errors.isEmpty(), "Browser page errors");
            System.out.println("D3_BROWSER_PASS request=" + id + " login=create=csrf=submit=reload=logout=true");
        }
    }
}
