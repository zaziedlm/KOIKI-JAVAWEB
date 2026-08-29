package org.koikifw.runtimeconsumer.workreview.application.usecase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.koikifw.runtimeconsumer.workreview.domain.model.WorkReview;
import org.koikifw.runtimeconsumer.workreview.domain.repository.WorkReviewRepository;

class RecordWorkReviewUseCaseTest {

    @Test
    void translatesOnlyDomainRejection() {
        RecordWorkReviewUseCase useCase = new RecordWorkReviewUseCase(new RejectingRepository());

        assertThrows(WorkReviewRejectedException.class,
                () -> useCase.record(UUID.randomUUID(), "x".repeat(101)));
    }

    @Test
    void doesNotTranslateRepositoryFailureAsBusinessRejection() {
        RecordWorkReviewUseCase useCase = new RecordWorkReviewUseCase(new RejectingRepository());

        assertThrowsExactly(IllegalArgumentException.class,
                () -> useCase.record(UUID.randomUUID(), "valid review"));
    }

    private static final class RejectingRepository implements WorkReviewRepository {

        @Override
        public Optional<WorkReview> findById(UUID workItemId) {
            return Optional.empty();
        }

        @Override
        public WorkReview save(WorkReview review) {
            throw new IllegalArgumentException("persistence mapping failed");
        }
    }
}
