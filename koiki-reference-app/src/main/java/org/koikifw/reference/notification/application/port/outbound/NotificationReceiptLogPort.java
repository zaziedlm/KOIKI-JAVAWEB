package org.koikifw.reference.notification.application.port.outbound;

import org.koikifw.reference.expense.domain.event.ExpenseApprovedEvent;

/** A durable record of a provider acceptance observed by Reference. */
public interface NotificationReceiptLogPort {
    void record(ExpenseApprovedEvent event, NotificationProviderPort.Receipt receipt);
}
