package org.koikifw.runtimeconsumer.workreview.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Tier 2 aggregate that owns review lifecycle invariants. */
@Entity
@Table(name = "kkbiz_work_review")
public class WorkReview {

    static final int MAX_LABEL_LENGTH = 100;

    @Id
    private UUID workItemId = UUID.randomUUID();

    private String label = "";

    @Enumerated(EnumType.STRING)
    private WorkReviewStatus status = WorkReviewStatus.PENDING;

    @Version
    private @Nullable Long version;

    protected WorkReview() {
    }

    public WorkReview(UUID workItemId, String label) {
        this.workItemId = Objects.requireNonNull(workItemId, "workItemId");
        this.label = requireValidLabel(label);
    }

    public void approve() {
        transitionTo(WorkReviewStatus.APPROVED);
    }

    public void reject() {
        transitionTo(WorkReviewStatus.REJECTED);
    }

    public UUID getWorkItemId() {
        return workItemId;
    }

    public String getLabel() {
        return label;
    }

    public WorkReviewStatus getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof WorkReview workReview
                && getWorkItemId().equals(workReview.getWorkItemId());
    }

    @Override
    public int hashCode() {
        return getWorkItemId().hashCode();
    }

    private void transitionTo(WorkReviewStatus next) {
        if (status != WorkReviewStatus.PENDING) {
            throw new IllegalStateException("review is already completed");
        }
        status = next;
    }

    private static String requireValidLabel(String label) {
        Objects.requireNonNull(label, "label");
        if (label.isBlank()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        if (label.length() > MAX_LABEL_LENGTH) {
            throw new IllegalArgumentException("label exceeds review limit");
        }
        return label;
    }
}
