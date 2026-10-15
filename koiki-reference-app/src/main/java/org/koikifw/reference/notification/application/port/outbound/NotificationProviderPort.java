package org.koikifw.reference.notification.application.port.outbound;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.koikifw.reference.expense.domain.event.ExpenseApprovedEvent;

public interface NotificationProviderPort {
    Receipt deliver(ExpenseApprovedEvent event);

    Inspection inspect(ExpenseApprovedEvent event);

    record Receipt(UUID acceptanceId) { }

    enum DeliveryFailureKind { EXPLICIT_REJECTION, UNKNOWN }

    final class DeliveryFailure extends IllegalStateException {
        private final DeliveryFailureKind kind;

        public DeliveryFailure(DeliveryFailureKind kind, String message) {
            super(message);
            this.kind = kind;
        }

        public DeliveryFailure(DeliveryFailureKind kind, String message, Throwable cause) {
            super(message, cause);
            this.kind = kind;
        }

        public DeliveryFailureKind kind() { return kind; }
    }

    enum InspectionState { ACCEPTED_MATCH, NOT_FOUND_WITHIN_VALID_WINDOW, CONFLICT, EXPIRED, UNKNOWN }

    record Inspection(InspectionState state, Optional<UUID> acceptanceId, Optional<Instant> validUntil) { }
}
