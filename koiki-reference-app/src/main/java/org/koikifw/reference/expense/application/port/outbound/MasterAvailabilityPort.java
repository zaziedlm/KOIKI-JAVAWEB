package org.koikifw.reference.expense.application.port.outbound;

import java.util.UUID;
import org.koikifw.identity.FrameworkUserId;

/** Expense-owned view of current Reference master availability. */
public interface MasterAvailabilityPort {

    boolean isActiveDepartment(UUID departmentId);

    boolean isActiveExpenseCategory(UUID expenseCategoryId);

    boolean isUserAssignedToActiveDepartment(FrameworkUserId userId, UUID departmentId);
}

