package org.koikifw.reference.notification.application.query;

import java.util.Objects;
import java.util.UUID;

/** One immutable publication snapshot supplied by the isolated R3-A operator fixture. */
public record IsolatedRecoverySelection(String environmentId, UUID publicationId, UUID eventId,
        String listenerId, String status, int completionAttempts, String notificationKey,
        String payloadSha256) {
    public IsolatedRecoverySelection {
        Objects.requireNonNull(environmentId, "environmentId");
        Objects.requireNonNull(publicationId, "publicationId");
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(listenerId, "listenerId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(notificationKey, "notificationKey");
        Objects.requireNonNull(payloadSha256, "payloadSha256");
        if (environmentId.isBlank() || listenerId.isBlank() || notificationKey.isBlank()
                || !listenerId.contains("ExpenseApprovalListener.onApproval")
                || !(status.equals("PUBLISHED") || status.equals("PROCESSING"))
                || completionAttempts < 0 || !payloadSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Invalid isolated recovery selection");
        }
    }
}
