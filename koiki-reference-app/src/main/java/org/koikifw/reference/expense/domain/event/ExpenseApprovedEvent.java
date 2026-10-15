package org.koikifw.reference.expense.domain.event;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/** Immutable approval facts; no entity, credentials, or request-thread state. */
public record ExpenseApprovedEvent(
        UUID eventId, UUID expenseRequestId, UUID applicantUserId,
        long approvalVersion, long claimedAmount) {
    public ExpenseApprovedEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(expenseRequestId, "expenseRequestId");
        Objects.requireNonNull(applicantUserId, "applicantUserId");
        if (approvalVersion < 1 || claimedAmount < 1) {
            throw new IllegalArgumentException("Approval version and amount must be positive");
        }
    }

    public String notificationKey() {
        return "expense-approved:v1:" + expenseRequestId + ":" + approvalVersion + ":applicant";
    }

    public String canonicalPayload() {
        return "{\"schemaVersion\":1,\"expenseRequestId\":\"" + expenseRequestId
                + "\",\"applicantUserId\":\"" + applicantUserId
                + "\",\"approvalVersion\":" + approvalVersion
                + ",\"claimedAmount\":" + claimedAmount + "}";
    }

    public String payloadSha256() {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonicalPayload().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
