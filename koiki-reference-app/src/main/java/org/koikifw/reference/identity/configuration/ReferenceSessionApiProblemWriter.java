package org.koikifw.reference.identity.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

/** Reference-owned, safe JSON failures raised before MVC is reached. */
final class ReferenceSessionApiProblemWriter {
    private final JsonMapper json = JsonMapper.builder().build();

    void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
            String detail, String code) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        json.writeValue(response.getOutputStream(), Map.of(
                "type", "about:blank", "title", status.getReasonPhrase(), "status", status.value(),
                "detail", detail, "instance", request.getRequestURI(), "code", code));
    }
}
