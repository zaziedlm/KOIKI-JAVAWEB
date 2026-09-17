package org.koikifw.reference.expense.adapter.outbound.master;

import java.util.Objects;
import java.util.UUID;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.reference.expense.application.port.outbound.MasterAvailabilityPort;
import org.koikifw.reference.master.contract.MasterAvailabilityQuery;
import org.springframework.stereotype.Component;

/** Keeps the ADR-049 cross-module query exception at one outbound boundary. */
@Component
public class MasterAvailabilityAdapter implements MasterAvailabilityPort {

    private final MasterAvailabilityQuery query;

    public MasterAvailabilityAdapter(MasterAvailabilityQuery query) {
        this.query = Objects.requireNonNull(query, "query");
    }

    @Override
    public boolean isActiveDepartment(UUID departmentId) {
        return query.isActiveDepartment(departmentId);
    }

    @Override
    public boolean isActiveExpenseCategory(UUID expenseCategoryId) {
        return query.isActiveExpenseCategory(expenseCategoryId);
    }

    @Override
    public boolean isUserAssignedToActiveDepartment(
            FrameworkUserId userId, UUID departmentId) {
        return query.isUserAssignedToActiveDepartment(userId, departmentId);
    }
}

