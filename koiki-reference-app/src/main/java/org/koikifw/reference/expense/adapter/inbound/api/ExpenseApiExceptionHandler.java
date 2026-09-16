package org.koikifw.reference.expense.adapter.inbound.api;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.koikifw.reference.expense.application.ExpenseFailure;
import org.koikifw.reference.expense.application.ExpenseOperationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps only failures reachable from the approved minimal expense API. */
@RestControllerAdvice(assignableTypes = ExpenseApiController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ExpenseApiExceptionHandler {

    @ExceptionHandler(ExpenseApiNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(
            ExpenseApiNotFoundException exception, HttpServletRequest request) {
        return response(
                HttpStatus.NOT_FOUND,
                "Expense request was not found.",
                "KOIKI-REF-EXPENSE-003",
                request);
    }

    @ExceptionHandler(ExpenseOperationException.class)
    ResponseEntity<ProblemDetail> handle(
            ExpenseOperationException exception, HttpServletRequest request) {
        ExpenseFailure failure = exception.failure();
        return switch (failure) {
            case INVALID_INPUT -> response(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Expense input violates a business constraint.",
                    "KOIKI-REF-EXPENSE-001",
                    request);
            case MASTER_UNAVAILABLE -> response(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "A required master value is unavailable.",
                    "KOIKI-REF-EXPENSE-002",
                    request);
            case NOT_FOUND -> response(
                    HttpStatus.NOT_FOUND,
                    "Expense request was not found.",
                    "KOIKI-REF-EXPENSE-003",
                    request);
            case INVALID_TRANSITION -> response(
                    HttpStatus.CONFLICT,
                    "The expense request cannot transition from its current state.",
                    "KOIKI-REF-EXPENSE-004",
                    request);
            case CONCURRENT_MODIFICATION -> response(
                    HttpStatus.CONFLICT,
                    "The expense request was modified concurrently.",
                    "KOIKI-REF-EXPENSE-005",
                    request);
            case CONFLICT -> response(
                    HttpStatus.CONFLICT,
                    "The expense request conflicts with current data.",
                    "KOIKI-REF-EXPENSE-006",
                    request);
            case DEPENDENCY_FAILURE -> response(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "A required expense dependency is unavailable.",
                    "KOIKI-REF-EXPENSE-007",
                    request);
            case SELF_DECISION -> throw exception;
        };
    }

    private static ResponseEntity<ProblemDetail> response(
            HttpStatus status, String detail, String code, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("about:blank"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        return ResponseEntity.status(status).body(problem);
    }
}
