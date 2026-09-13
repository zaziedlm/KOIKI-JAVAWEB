package org.koikifw.reference.expense.application;

import java.util.Objects;
import java.util.UUID;
import org.koikifw.reference.expense.domain.model.ExpenseStatus;
import org.koikifw.reference.expense.domain.repository.ExpenseRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Enforces the expense-owned pending-request invariant for department deactivation. */
@Service
public class DepartmentDeactivationGuard {

    private final ExpenseRequestRepository requests;

    public DepartmentDeactivationGuard(ExpenseRequestRepository requests) {
        this.requests = Objects.requireNonNull(requests, "requests");
    }

    @Transactional(readOnly = true)
    public void ensureAllowed(UUID departmentId) {
        Objects.requireNonNull(departmentId, "departmentId");
        if (requests.existsByDepartmentIdAndStatusIn(
                departmentId, ExpenseStatus.pendingStatuses())) {
            throw new PendingExpenseRequestsException();
        }
    }

    private static final class PendingExpenseRequestsException extends IllegalStateException {

        private static final long serialVersionUID = 1L;

        private PendingExpenseRequestsException() {
            super("Pending expense requests prevent department deactivation.");
        }
    }
}
