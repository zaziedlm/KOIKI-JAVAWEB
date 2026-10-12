package org.koikifw.reference.expense.application.port.outbound;

import org.koikifw.reference.expense.domain.event.ExpenseApprovedEvent;

/** Publishes approval facts inside the original business transaction. */
public interface ExpenseApprovedEventPort {
    void publish(ExpenseApprovedEvent event);
}
