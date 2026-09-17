package org.koikifw.reference.identity.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;

/** Writes safe authentication failures before MVC exception handling is available. */
final class ReferenceApiSecurityProblemWriter {

    private final ObjectMapper objectMapper;

    ReferenceApiSecurityProblemWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    void unauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        write(request, response, HttpStatus.UNAUTHORIZED,
                "Bearer authentication is required.", "KOIKI-REF-AUTH-001");
    }

    void forbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
        write(request, response, HttpStatus.FORBIDDEN,
                "The authenticated user is not authorized.", "KOIKI-REF-AUTH-002");
    }

    private void write(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String detail,
            String code) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("about:blank"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
