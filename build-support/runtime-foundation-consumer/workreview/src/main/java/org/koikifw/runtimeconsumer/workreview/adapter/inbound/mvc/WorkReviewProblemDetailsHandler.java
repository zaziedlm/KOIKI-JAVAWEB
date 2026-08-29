package org.koikifw.runtimeconsumer.workreview.adapter.inbound.mvc;

import java.net.URI;
import org.koikifw.runtimeconsumer.workreview.application.usecase.WorkReviewRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps workreview business rejection to the Customer application's HTTP contract. */
@RestControllerAdvice
public final class WorkReviewProblemDetailsHandler {

    private static final String REJECTION_CODE = "WORKREVIEW-001";
    private static final String SAFE_DETAIL = "Work item could not be submitted for review.";
    private static final URI ABOUT_BLANK = URI.create("about:blank");

    @ExceptionHandler(WorkReviewRejectedException.class)
    ProblemDetail handleRejection() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_CONTENT, SAFE_DETAIL);
        problem.setType(ABOUT_BLANK);
        problem.setProperty("code", REJECTION_CODE);
        return problem;
    }
}
