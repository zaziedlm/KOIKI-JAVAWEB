package org.koikifw.reference.notification.adapter.inbound.event;

import java.util.Objects;
import org.koikifw.reference.expense.domain.event.ExpenseApprovedEvent;
import org.koikifw.reference.notification.application.NotificationDispatchService;
import org.springframework.modulith.events.ApplicationModuleListener;

public class ExpenseApprovalListener {
    private final NotificationDispatchService service;

    public ExpenseApprovalListener(NotificationDispatchService service) {
        this.service = Objects.requireNonNull(service);
    }

    @ApplicationModuleListener
    public void onApproval(ExpenseApprovedEvent event) {
        service.dispatch(event);
    }
}
