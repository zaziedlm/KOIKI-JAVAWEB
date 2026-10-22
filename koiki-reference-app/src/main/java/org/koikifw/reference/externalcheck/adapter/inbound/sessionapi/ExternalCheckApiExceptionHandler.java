package org.koikifw.reference.externalcheck.adapter.inbound.sessionapi;

import org.koikifw.reference.externalcheck.application.ExternalCheckFailure;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import java.util.concurrent.RejectedExecutionException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Safe, narrow external-check errors. Security and unexpected failures retain their existing owners. */
@RestControllerAdvice(assignableTypes = ExternalCheckController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "koiki.reference.external-check.enabled", havingValue = "true")
public class ExternalCheckApiExceptionHandler {
    @ExceptionHandler(ExternalCheckFailure.class)
    ResponseEntity<ProblemDetail> failure(ExternalCheckFailure failure) {
        int status = switch (failure.kind()) {
            case INVALID_INPUT -> 400;
            case PROVIDER_REJECTED, INVALID_PROVIDER_RESPONSE -> 502;
            case PROVIDER_TIMEOUT -> 504;
            default -> 503;
        };
        return response(status, failure.kind().name());
    }

    @ExceptionHandler(RejectedExecutionException.class)
    ResponseEntity<ProblemDetail> saturated(RejectedExecutionException failure) {
        return response(503, "CONCURRENCY_LIMIT");
    }

    private static ResponseEntity<ProblemDetail> response(int status, String category) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), "External reference check could not be completed.");
        problem.setProperty("code", "KOIKI-REF-EXTERNAL-" + category);
        return ResponseEntity.status(status).body(problem);
    }
}
