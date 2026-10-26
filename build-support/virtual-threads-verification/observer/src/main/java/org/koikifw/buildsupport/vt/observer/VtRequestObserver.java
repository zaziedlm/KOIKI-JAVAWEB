package org.koikifw.buildsupport.vt.observer;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** Samples existing synchronous requests without altering identity, MDC or HTTP responses. */
final class VtRequestObserver extends OncePerRequestFilter implements HandlerInterceptor, AutoCloseable {
    private final ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(256);
    private final AtomicReference<String> failure = new AtomicReference<>();
    private final AtomicInteger records = new AtomicInteger();
    private final AtomicInteger requests = new AtomicInteger();
    private final BufferedWriter output;
    private final Thread writer;
    private volatile boolean closing;

    VtRequestObserver(Path path) throws IOException {
        output = Files.newBufferedWriter(path, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE_NEW);
        writer = Thread.ofPlatform().name("d2-observation-writer").daemon(true).start(this::write);
    }

    void record(String line) {
        if (records.incrementAndGet() > 30000 || !queue.offer(line)) failure.compareAndSet(null, "OUTPUT_LIMIT_OR_QUEUE_OVERFLOW");
    }

    private void write() {
        long bytes = 0;
        try {
            while (!closing || !queue.isEmpty()) {
                String line = queue.poll(100, TimeUnit.MILLISECONDS);
                if (line != null) {
                    bytes += line.getBytes(StandardCharsets.UTF_8).length + 1;
                    if (bytes > 32L * 1024 * 1024) failure.compareAndSet(null, "OUTPUT_BYTE_LIMIT");
                    else { output.write(line); output.newLine(); output.flush(); }
                }
            }
            if (failure.get() != null) { output.write("FAIL|" + failure.get()); output.newLine(); }
            output.write("END|" + records.get()); output.newLine();
            output.close();
        } catch (Exception exception) {
            failure.compareAndSet(null, "OUTPUT_WRITE_FAILURE");
            logger.error("D2 observation write failed");
            try { output.close(); } catch (IOException ignored) { /* failure already recorded */ }
        }
    }

    private static String safe(String value) {
        return value == null ? "-" : value.matches("[A-Za-z0-9._:-]{1,128}") ? value : "INVALID";
    }

    private static String actor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return "-";
        String name = authentication.getName();
        return name.matches("[0-9a-f-]{36}") ? name : "NON_FIXTURE_ACTOR";
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String beforeId = MDC.get("requestId");
        var beforeAuth = SecurityContextHolder.getContext().getAuthentication();
        try { chain.doFilter(request, response); }
        finally {
            boolean restored = Objects.equals(beforeId, MDC.get("requestId"))
                    && Objects.equals(beforeAuth, SecurityContextHolder.getContext().getAuthentication());
            record("F|" + safe(response.getHeader("X-Request-ID")) + "|" + restored + "|" + response.getStatus()
                    + "|" + safe(request.getHeader("X-Request-ID")));
        }
    }

    @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod method && method.getBeanType().getName().equals(
                "org.koikifw.reference.expense.adapter.inbound.sessionapi.ExpenseSessionApiController")) {
            requests.incrementAndGet();
            request.setAttribute("d2.observed", Boolean.TRUE);
            record("H|" + safe(MDC.get("requestId")) + "|" + actor() + "|" + Thread.currentThread().isVirtual()
                    + "|" + Thread.currentThread().threadId() + "|" + method.getMethod().getName());
        }
        return true;
    }

    @Override public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        if (Boolean.TRUE.equals(request.getAttribute("d2.observed"))) {
            requests.decrementAndGet();
            record("C|" + safe(MDC.get("requestId")) + "|" + actor() + "|" + response.getStatus());
        }
    }

    int activeRequests() { return requests.get(); }

    @Override public void close() throws InterruptedException {
        closing = true;
        writer.join(5000);
        if (writer.isAlive() || failure.get() != null) throw new IllegalStateException("D2 observer output incomplete");
    }
}
