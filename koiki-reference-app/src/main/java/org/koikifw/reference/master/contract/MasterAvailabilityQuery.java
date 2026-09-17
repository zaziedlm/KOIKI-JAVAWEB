package org.koikifw.reference.master.contract;

import java.util.UUID;
import org.koikifw.identity.FrameworkUserId;

/** Reports only current master availability required by another Reference module. */
public interface MasterAvailabilityQuery {

    boolean isActiveDepartment(UUID departmentId);

    boolean isActiveExpenseCategory(UUID expenseCategoryId);

    boolean isUserAssignedToActiveDepartment(
            FrameworkUserId userId, UUID departmentId);
}
