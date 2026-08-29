package org.koikifw.runtimeconsumer.workreview.application.usecase;

/** Application-level rejection that does not expose domain implementation details. */
public final class WorkReviewRejectedException extends RuntimeException {

    public WorkReviewRejectedException(IllegalArgumentException cause) {
        super("Work review rejected the work item.", cause);
    }
}
