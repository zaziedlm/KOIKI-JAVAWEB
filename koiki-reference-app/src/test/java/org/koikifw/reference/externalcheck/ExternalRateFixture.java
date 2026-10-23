package org.koikifw.reference.externalcheck;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.koikifw.reference.externalcheck.application.ExternalCheckService;
import org.koikifw.reference.externalcheck.configuration.ExternalCheckConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.resilience.annotation.EnableResilientMethods;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** Tooling-owned finite fault fixture; no production endpoint or failure switch. */
final class ExternalRateFixture implements AutoCloseable {
    enum Mode { NORMAL, TRANSIENT, UNAVAILABLE, BAD_REQUEST, MISSING, INVALID, OVERSIZE, RATE_LIMIT,
        HEADER_DELAY, BODY_DELAY, HOLD }
    final AtomicInteger requests = new AtomicInteger();
    final AtomicInteger active = new AtomicInteger();
    final AtomicInteger maximum = new AtomicInteger();
    final ExecutorService workers = Executors.newFixedThreadPool(3);
    final HttpServer server;
    volatile Mode mode = Mode.NORMAL;
    volatile String rawBody = "";
    volatile CountDownLatch entered = new CountDownLatch(1);
    final CountDownLatch release = new CountDownLatch(1);

    ExternalRateFixture() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 2);
        server.setExecutor(workers);
        server.createContext("/rates", this::handle);
        server.start();
    }

    URI baseUri() { return URI.create("http://127.0.0.1:" + server.getAddress().getPort()); }

    private void handle(HttpExchange exchange) throws IOException {
        int attempt = requests.incrementAndGet();
        if (attempt > 64) { exchange.close(); throw new IllegalStateException("Fixture request limit exceeded"); }
        maximum.accumulateAndGet(active.incrementAndGet(), Math::max);
        entered.countDown();
        try (exchange) {
            if (!"GET".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }
            String query = exchange.getRequestURI().getQuery();
            String base = query != null && query.contains("base=EUR") ? "EUR" : "USD";
            String body = rawBody.isEmpty() ? "{\"base\":\"" + base
                    + "\",\"quote\":\"JPY\",\"date\":\"2026-10-22\",\"rate\":\"150.12345678\"}" : rawBody;
            int status = switch (mode) {
                case TRANSIENT -> attempt < 3 ? 503 : 200;
                case UNAVAILABLE -> 503;
                case BAD_REQUEST -> 400;
                case MISSING -> 404;
                case RATE_LIMIT -> 429;
                default -> 200;
            };
            if (mode == Mode.HEADER_DELAY || mode == Mode.HOLD) { release.await(1500, TimeUnit.MILLISECONDS); }
            if (mode == Mode.INVALID) { body = "{invalid"; }
            if (mode == Mode.OVERSIZE) { body = " ".repeat(4097); }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Retry-After", "60");
            exchange.sendResponseHeaders(status, bytes.length);
            if (mode == Mode.BODY_DELAY) {
                exchange.getResponseBody().write(bytes, 0, 1);
                exchange.getResponseBody().flush();
                release.await(1500, TimeUnit.MILLISECONDS);
                exchange.getResponseBody().write(bytes, 1, bytes.length - 1);
            } else { exchange.getResponseBody().write(bytes); }
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
        } catch (IOException failure) {
            // A client timeout/abort can close the socket; finite cleanup is still required below.
        } finally { active.decrementAndGet(); }
    }

    AnnotationConfigApplicationContext context() { return context(Map.of()); }

    AnnotationConfigApplicationContext context(Map<String, Object> overrides) {
        var context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setConversionService(new ApplicationConversionService());
        var values = new HashMap<String, Object>();
        values.put("koiki.reference.external-check.enabled", "true");
        values.put("koiki.reference.external-check.provider-base-uri", baseUri().toString());
        values.putAll(overrides);
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("c2Test", values));
        new org.koikifw.starter.api.internal.KoikiApiDefaultsEnvironmentPostProcessor()
                .postProcessEnvironment(context.getEnvironment(), new SpringApplication());
        context.register(ResilienceFixture.class, ExternalCheckConfiguration.class);
        context.refresh();
        return context;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableResilientMethods
    static class ResilienceFixture {
        @Bean ObjectMapper externalTestMapper() { return JsonMapper.builder().build(); }
    }

    @Override
    public void close() {
        release.countDown();
        server.stop(0);
        workers.shutdownNow();
        try {
            if (!workers.awaitTermination(Duration.ofSeconds(2).toMillis(), TimeUnit.MILLISECONDS) || active.get() != 0) {
                throw new IllegalStateException("Fixture workers did not stop");
            }
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Fixture cleanup interrupted");
        }
    }
}
