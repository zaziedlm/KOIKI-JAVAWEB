package org.koikifw.reference.notification.application;

import java.util.Objects;
import java.time.Instant;
import org.koikifw.reference.expense.domain.event.ExpenseApprovedEvent;
import org.koikifw.reference.notification.application.port.outbound.NotificationProviderPort;
import org.koikifw.reference.notification.application.port.outbound.NotificationReceiptLogPort;

public class NotificationDispatchService {
    private final NotificationProviderPort provider;
    private final NotificationReceiptLogPort receiptLog;
    private final boolean recovery;

    public NotificationDispatchService(NotificationProviderPort provider, NotificationReceiptLogPort receiptLog,
            boolean recovery) {
        this.provider = Objects.requireNonNull(provider);
        this.receiptLog = Objects.requireNonNull(receiptLog);
        this.recovery = recovery;
    }

    public void dispatch(ExpenseApprovedEvent event) {
        if (recovery) {
            var inspection = provider.inspect(event);
            if (inspection.state() == NotificationProviderPort.InspectionState.ACCEPTED_MATCH
                    && inspection.acceptanceId().isPresent()) {
                receiptLog.record(event, new NotificationProviderPort.Receipt(inspection.acceptanceId().orElseThrow()));
                return;
            }
            if (inspection.state() != NotificationProviderPort.InspectionState.NOT_FOUND_WITHIN_VALID_WINDOW
                    || inspection.validUntil().isEmpty()
                    || !inspection.validUntil().orElseThrow().isAfter(Instant.now())) {
                throw new IllegalStateException("Provider result requires HOLD");
            }
        }
        receiptLog.record(event, provider.deliver(event));
    }
}
