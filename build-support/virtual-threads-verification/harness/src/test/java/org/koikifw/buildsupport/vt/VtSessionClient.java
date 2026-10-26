package org.koikifw.buildsupport.vt;

import static org.junit.jupiter.api.Assertions.*;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;

/** Independent real Session cookies per user/worker. Never prints bodies or credentials. */
final class VtSessionClient implements AutoCloseable {
    final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    final HttpClient http = HttpClient.newBuilder().cookieHandler(cookies)
            .connectTimeout(Duration.ofSeconds(1)).followRedirects(HttpClient.Redirect.NEVER).build();
    final VtReferenceFixture fixture;

    VtSessionClient(VtReferenceFixture fixture) { this.fixture = fixture; }

    void login(String email) throws Exception {
        String html = get("/login").body();
        var matcher = Pattern.compile("<input[^>]*name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find(), "Login CSRF hidden input missing");
        String csrf = matcher.group(1); fixture.secrets.add(csrf);
        String body = "username=" + encode(email) + "&password=" + encode(fixture.password) + "&_csrf=" + encode(csrf);
        var response = http.send(HttpRequest.newBuilder(URI.create(fixture.base + "/login"))
                .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.discarding());
        assertEquals(302, response.statusCode(), "Login must redirect");
        assertEquals("/", URI.create(fixture.base).resolve(response.headers().firstValue("location").orElseThrow()).getPath());
        remember();
        assertEquals(204, get("/spa-api/csrf").statusCode());
        remember();
    }

    HttpResponse<String> get(String path) throws Exception { return get(path, UUID.randomUUID().toString()); }

    HttpResponse<String> get(String path, String requestId) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(fixture.base + path)).timeout(Duration.ofSeconds(10));
        if (requestId != null) request.header("X-Request-ID", requestId);
        var response = http.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
        remember(); return response;
    }

    HttpResponse<String> post(String path, String body, String csrf) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(fixture.base + path)).timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json").header("X-Request-ID", UUID.randomUUID().toString());
        if (csrf != null) request.header("X-XSRF-TOKEN", csrf);
        var response = http.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        remember(); return response;
    }

    String csrf() {
        return cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("XSRF-TOKEN"))
                .findFirst().orElseThrow().getValue();
    }

    String session() {
        return cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("SESSION"))
                .findFirst().orElseThrow().getValue();
    }

    void remember() { cookies.getCookieStore().getCookies().forEach(cookie -> fixture.secrets.add(cookie.getValue())); }

    static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    @Override public void close() { http.close(); cookies.getCookieStore().removeAll(); }
}
