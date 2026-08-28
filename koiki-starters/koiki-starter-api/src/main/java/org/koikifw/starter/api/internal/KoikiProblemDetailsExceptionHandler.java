package org.koikifw.starter.api.internal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;

/** Internal RFC 9457 mapping that keeps exception implementation details out of API responses. */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
final class KoikiProblemDetailsExceptionHandler extends ResponseEntityExceptionHandler {

    static final String CODE_PROPERTY = "code";
    static final String VIOLATIONS_PROPERTY = "violations";
    static final String VALIDATION_CODE = "KOIKI-VALIDATION-001";
    static final String INVALID_JSON_CODE = "KOIKI-JSON-001";
    static final String JSON_PROCESSING_CODE = "KOIKI-JSON-002";
    static final String INTERNAL_CODE = "KOIKI-INTERNAL-001";
    private static final URI ABOUT_BLANK = URI.create("about:blank");

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problem = requestProblem(
                status, "Request validation failed.", VALIDATION_CODE);
        problem.setProperty(VIOLATIONS_PROPERTY, bindingViolations(exception));
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problem = requestProblem(
                status, "Request validation failed.", VALIDATION_CODE);
        problem.setProperty(VIOLATIONS_PROPERTY, methodViolations(exception));
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        boolean causedByJackson = hasCause(exception, JacksonException.class);
        String detail = causedByJackson
                ? "Request body is not valid JSON."
                : "Request body could not be read.";
        String code = causedByJackson ? INVALID_JSON_CODE : "KOIKI-REQUEST-001";
        ProblemDetail problem = requestProblem(status, detail, code);
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @ExceptionHandler(JacksonException.class)
    ResponseEntity<Object> handleJacksonException(JacksonException exception, WebRequest request) {
        logger.warn("Jackson processing failed", exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "JSON processing failed.");
        problem.setProperty(CODE_PROPERTY, JSON_PROCESSING_CODE);
        return createResponseEntity(
                problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpectedException(Exception exception, WebRequest request) {
        logger.error("Unexpected request processing failure", exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        problem.setProperty(CODE_PROPERTY, INTERNAL_CODE);
        return createResponseEntity(
                problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        if (body instanceof ProblemDetail problem && !hasCode(problem)) {
            problem.setProperty(CODE_PROPERTY, "KOIKI-HTTP-" + status.value());
        }
        if (body instanceof ProblemDetail problem && problem.getType() == null) {
            problem.setType(ABOUT_BLANK);
        }
        return super.createResponseEntity(body, headers, status, request);
    }

    private static ProblemDetail requestProblem(HttpStatusCode status, String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty(CODE_PROPERTY, code);
        return problem;
    }

    private List<Map<String, String>> bindingViolations(MethodArgumentNotValidException exception) {
        List<Map<String, String>> violations = new ArrayList<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            violations.add(violation(fieldError.getField(), resolveMessage(fieldError)));
        }
        for (ObjectError globalError : exception.getBindingResult().getGlobalErrors()) {
            violations.add(violation(globalError.getObjectName(), resolveMessage(globalError)));
        }
        return List.copyOf(violations);
    }

    private List<Map<String, String>> methodViolations(HandlerMethodValidationException exception) {
        List<Map<String, String>> violations = new ArrayList<>();
        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            String parameter = result.getMethodParameter().getParameterName();
            if (parameter == null) {
                parameter = "arg" + result.getMethodParameter().getParameterIndex();
            }
            for (var error : result.getResolvableErrors()) {
                violations.add(violation(parameter, resolveMessage(error)));
            }
        }
        for (var error : exception.getCrossParameterValidationResults()) {
            violations.add(violation("request", resolveMessage(error)));
        }
        return List.copyOf(violations);
    }

    private String resolveMessage(org.springframework.context.MessageSourceResolvable error) {
        MessageSource messageSource = getMessageSource();
        Locale locale = LocaleContextHolder.getLocale();
        if (messageSource != null) {
            return messageSource.getMessage(error, locale);
        }
        String defaultMessage = error.getDefaultMessage();
        return defaultMessage == null ? "Invalid value." : defaultMessage;
    }

    private static Map<String, String> violation(String field, String message) {
        return Map.of("field", field, "message", message);
    }

    private static boolean hasCause(Throwable exception, Class<? extends Throwable> expectedType) {
        Throwable current = exception;
        while (current != null) {
            if (expectedType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean hasCode(ProblemDetail problem) {
        Map<String, Object> properties = problem.getProperties();
        return properties != null && properties.containsKey(CODE_PROPERTY);
    }
}
