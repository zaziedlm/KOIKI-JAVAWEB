package org.koikifw.reference.identity.adapter.inbound.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.koikifw.identity.IdentityFailure;
import org.koikifw.identity.IdentityOperationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

class IdentityManagementExceptionHandlerTest {

    private final IdentityManagementExceptionHandler handler =
            new IdentityManagementExceptionHandler();

    @ParameterizedTest
    @MethodSource("safeFailureMappings")
    void mapsFrameworkFailureToSanitizedHtmlResponse(
            IdentityFailure failure, HttpStatus status, String message) {
        ModelAndView response =
                handler.handleIdentityOperation(new IdentityOperationException(failure));

        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getViewName()).isEqualTo("identity/error");
        assertThat(response.getModel()).containsEntry("message", message);
        assertThat(response.getModel().toString()).doesNotContain("Identity operation failed");
    }

    private static Stream<Arguments> safeFailureMappings() {
        return Stream.of(
                Arguments.of(
                        IdentityFailure.INVALID_INPUT,
                        HttpStatus.BAD_REQUEST,
                        "The role change request is invalid."),
                Arguments.of(
                        IdentityFailure.NOT_FOUND,
                        HttpStatus.NOT_FOUND,
                        "The identity user or role was not found."),
                Arguments.of(
                        IdentityFailure.CONFLICT,
                        HttpStatus.CONFLICT,
                        "Identity data changed. Reload the user and try again."),
                Arguments.of(
                        IdentityFailure.CONCURRENT_MODIFICATION,
                        HttpStatus.CONFLICT,
                        "Identity data changed. Reload the user and try again."),
                Arguments.of(
                        IdentityFailure.DEPENDENCY_FAILURE,
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "The identity service is temporarily unavailable. Try again later."));
    }
}
