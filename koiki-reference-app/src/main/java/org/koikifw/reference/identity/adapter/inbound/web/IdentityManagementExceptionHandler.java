package org.koikifw.reference.identity.adapter.inbound.web;

import org.koikifw.identity.IdentityFailure;
import org.koikifw.identity.IdentityOperationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/** Maps Framework Identity failures to sanitized Reference HTML responses. */
@ControllerAdvice(assignableTypes = IdentityManagementController.class)
public class IdentityManagementExceptionHandler {

    @ExceptionHandler(IdentityOperationException.class)
    ModelAndView handleIdentityOperation(IdentityOperationException exception) {
        IdentityFailure failure = exception.failure();
        HttpStatus status = status(failure);
        ModelAndView response = new ModelAndView("identity/error");
        response.setStatus(status);
        response.addObject("message", message(failure));
        return response;
    }

    private static HttpStatus status(IdentityFailure failure) {
        return switch (failure) {
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT, CONCURRENT_MODIFICATION -> HttpStatus.CONFLICT;
            case DEPENDENCY_FAILURE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
    }

    private static String message(IdentityFailure failure) {
        return switch (failure) {
            case INVALID_INPUT -> "The role change request is invalid.";
            case NOT_FOUND -> "The identity user or role was not found.";
            case CONFLICT, CONCURRENT_MODIFICATION ->
                "Identity data changed. Reload the user and try again.";
            case DEPENDENCY_FAILURE ->
                "The identity service is temporarily unavailable. Try again later.";
        };
    }
}
