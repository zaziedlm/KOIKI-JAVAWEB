package org.koikifw.reference.expense.adapter.outbound.event;

import java.util.Objects;
import org.koikifw.reference.expense.application.port.outbound.ExpenseApprovedEventPort;
import org.koikifw.reference.expense.domain.event.ExpenseApprovedEvent;
import org.springframework.context.ApplicationEventPublisher;

public final class SpringExpenseApprovedEventAdapter implements ExpenseApprovedEventPort {
    private final ApplicationEventPublisher publisher;

    public SpringExpenseApprovedEventAdapter(ApplicationEventPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher);
    }

    @Override
    public void publish(ExpenseApprovedEvent event) {
        publisher.publishEvent(event);
    }
}
