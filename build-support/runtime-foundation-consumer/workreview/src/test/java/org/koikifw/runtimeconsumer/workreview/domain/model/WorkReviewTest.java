package org.koikifw.runtimeconsumer.workreview.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class WorkReviewTest {

    @Test
    void ownsAOneWayReviewLifecycle() {
        WorkReview review = new WorkReview(UUID.randomUUID(), "review me");

        review.approve();

        assertEquals(WorkReviewStatus.APPROVED, review.getStatus());
        assertThrows(IllegalStateException.class, review::reject);
    }

    @Test
    void rejectsLabelsOutsideItsBusinessLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> new WorkReview(UUID.randomUUID(), "x".repeat(101)));
    }

    @Test
    void usesIdentifierIdentityAcrossProxyCompatibleSubtypes() {
        UUID workItemId = UUID.randomUUID();
        WorkReview review = new WorkReview(workItemId, "review me");
        WorkReview proxyEquivalent = new WorkReviewProxy(workItemId, "loaded lazily");

        assertEquals(review, proxyEquivalent);
        assertEquals(proxyEquivalent, review);
        assertEquals(review.hashCode(), proxyEquivalent.hashCode());
        assertNotEquals(review, new WorkReview(UUID.randomUUID(), "review me"));
    }

    private static final class WorkReviewProxy extends WorkReview {

        private WorkReviewProxy(UUID workItemId, String label) {
            super(workItemId, label);
        }
    }
}
