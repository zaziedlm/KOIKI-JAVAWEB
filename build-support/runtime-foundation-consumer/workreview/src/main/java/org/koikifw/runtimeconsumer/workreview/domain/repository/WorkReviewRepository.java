package org.koikifw.runtimeconsumer.workreview.domain.repository;

import java.util.Optional;
import java.util.UUID;
import org.koikifw.runtimeconsumer.workreview.domain.model.WorkReview;
import org.springframework.data.repository.Repository;

/** Repository port owned by the Tier 2 domain. */
public interface WorkReviewRepository extends Repository<WorkReview, UUID> {

    Optional<WorkReview> findById(UUID workItemId);

    WorkReview save(WorkReview review);
}
