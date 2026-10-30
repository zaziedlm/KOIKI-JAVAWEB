package org.koikifw.buildsupport.d1;

import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Observes the existing HTTP span; deliberately creates no spans or propagation hooks. */
@RestController
public class D1Controller {
    private final Tracer tracer;
    public D1Controller(Tracer tracer) { this.tracer = tracer; }

    @RequestMapping({"/d1/ok", "/d1/fail"})
    public ResponseEntity<Map<String, String>> handle(HttpServletRequest request) {
        var span = tracer.currentSpan();
        if (span == null) { throw new IllegalStateException("No standard HTTP server span"); }
        String marker = request.getHeader("X-D1-Marker");
        if (marker == null || !marker.matches("[a-zA-Z0-9-]{1,60}")) { marker = "unmarked"; }
        Map<String, String> observed = Map.of(
                "marker", marker, "traceId", span.context().traceId(), "spanId", span.context().spanId(),
                "requestId", value("requestId"), "mdcTraceId", value("traceId"),
                "mdcSpanId", value("spanId"), "virtual", Boolean.toString(Thread.currentThread().isVirtual()));
        LoggerFactory.getLogger(D1Controller.class).info("D1_HANDLER {}", observed);
        return ResponseEntity.status(request.getRequestURI().endsWith("/fail") ? 500 : 200).body(observed);
    }
    private static String value(String name) { String v = MDC.get(name); return v == null ? "" : v; }
}
