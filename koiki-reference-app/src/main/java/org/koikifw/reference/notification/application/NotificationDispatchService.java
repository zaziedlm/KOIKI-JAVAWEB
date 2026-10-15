package org.koikifw.reference.notification.application;

import java.util.Objects;
import org.koikifw.reference.expense.domain.event.ExpenseApprovedEvent;
import org.koikifw.reference.notification.application.port.outbound.NotificationProviderPort;

public class NotificationDispatchService {
    private final NotificationProviderPort provider;

    public NotificationDispatchService(NotificationProviderPort provider) {
        this.provider = Objects.requireNonNull(provider);
    }

    public void dispatch(ExpenseApprovedEvent event) {
        provider.deliver(event);
    }
}
