package org.koikifw.archunit.fixture.v1.violation.rich.adapter.inbound.event;

import org.koikifw.archunit.fixture.v1.violation.rich.domain.model.Expense;
import org.springframework.context.event.EventListener;

public class LeakingListener {
    @EventListener
    public void on(Expense expense) {
    }
}
