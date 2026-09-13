package org.koikifw.reference.expense.adapter.inbound.event;

import java.util.Objects;
import org.koikifw.reference.expense.application.DepartmentDeactivationGuard;
import org.koikifw.reference.master.domain.event.DepartmentDeactivating;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Synchronously delegates the master event to the expense application guard. */
@Component
public class DepartmentDeactivatingListener {

    private final DepartmentDeactivationGuard guard;

    public DepartmentDeactivatingListener(DepartmentDeactivationGuard guard) {
        this.guard = Objects.requireNonNull(guard, "guard");
    }

    @EventListener
    public void on(DepartmentDeactivating event) {
        guard.ensureAllowed(event.departmentId());
    }
}
