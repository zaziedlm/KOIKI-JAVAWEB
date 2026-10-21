package org.koikifw.reference.expense.adapter.inbound.sessionapi;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.koikifw.reference.expense.application.ExpenseOperationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Handles only Session transport and expense failures; Security retains its own handlers. */
@RestControllerAdvice(assignableTypes = ExpenseSessionApiController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SessionExpenseApiExceptionHandler {
    @ExceptionHandler(SessionExpenseNotFoundException.class)
    ResponseEntity<ProblemDetail> missing(SessionExpenseNotFoundException exception, HttpServletRequest request) {
        return response(404, "Expense request was not found.", "KOIKI-REF-EXPENSE-003", request);
    }

    @ExceptionHandler(SessionExpenseInputException.class)
    ResponseEntity<ProblemDetail> invalid(SessionExpenseInputException exception, HttpServletRequest request) {
        return response(400, "Request validation failed.", "KOIKI-VALIDATION-001", request);
    }

    @ExceptionHandler(ExpenseOperationException.class)
    ResponseEntity<ProblemDetail> expense(ExpenseOperationException exception, HttpServletRequest request) {
        return switch (exception.failure()) {
            case INVALID_INPUT -> response(422, "Expense input violates a business constraint.",
                    "KOIKI-REF-EXPENSE-001", request);
            case MASTER_UNAVAILABLE -> response(422, "A required master value is unavailable.",
                    "KOIKI-REF-EXPENSE-002", request);
            case NOT_FOUND -> response(404, "Expense request was not found.", "KOIKI-REF-EXPENSE-003", request);
            case INVALID_TRANSITION -> response(409, "The expense request cannot transition from its current state.",
                    "KOIKI-REF-EXPENSE-004", request);
            case CONCURRENT_MODIFICATION -> response(409, "The expense request was modified concurrently.",
                    "KOIKI-REF-EXPENSE-005", request);
            case CONFLICT -> response(409, "The expense request conflicts with current data.",
                    "KOIKI-REF-EXPENSE-006", request);
            case DEPENDENCY_FAILURE -> response(503, "A required expense dependency is unavailable.",
                    "KOIKI-REF-EXPENSE-007", request);
            case SELF_DECISION -> throw exception;
        };
    }

    private static ResponseEntity<ProblemDetail> response(int status, String detail, String code,
            HttpServletRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
        problem.setType(URI.create("about:blank"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        return ResponseEntity.status(status).body(problem);
    }
}
