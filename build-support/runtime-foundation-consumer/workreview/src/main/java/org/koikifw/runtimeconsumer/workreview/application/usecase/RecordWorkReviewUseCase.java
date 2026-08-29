package org.koikifw.runtimeconsumer.workreview.application.usecase;

import java.util.Objects;
import java.util.UUID;
import org.koikifw.runtimeconsumer.workreview.domain.model.WorkReview;
import org.koikifw.runtimeconsumer.workreview.domain.repository.WorkReviewRepository;
import org.springframework.transaction.annotation.Transactional;

/** Records a pending review when a collaborating module creates a work item. */
public class RecordWorkReviewUseCase {

    private final WorkReviewRepository repository;

    public RecordWorkReviewUseCase(WorkReviewRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Transactional
    public void record(UUID workItemId, String label) {
        WorkReview review;
        try {
            review = new WorkReview(workItemId, label);
        } catch (IllegalArgumentException exception) {
            throw new WorkReviewRejectedException(exception);
        }
        repository.save(review);
    }
}
