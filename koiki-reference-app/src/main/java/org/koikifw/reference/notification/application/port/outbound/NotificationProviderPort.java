package org.koikifw.reference.notification.application.port.outbound;

import org.koikifw.reference.expense.domain.event.ExpenseApprovedEvent;

public interface NotificationProviderPort {
    void deliver(ExpenseApprovedEvent event);
}
