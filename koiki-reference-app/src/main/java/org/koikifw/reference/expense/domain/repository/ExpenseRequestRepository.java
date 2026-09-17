package org.koikifw.reference.expense.domain.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.koikifw.reference.expense.domain.model.ExpenseRequest;
import org.koikifw.reference.expense.domain.model.ExpenseStatus;
import org.springframework.data.repository.Repository;

/** Spring Data Commons repository port for the aggregate root. */
public interface ExpenseRequestRepository extends Repository<ExpenseRequest, UUID> {

    Optional<ExpenseRequest> findById(UUID expenseRequestId);

    ExpenseRequest save(ExpenseRequest expenseRequest);

    boolean existsByDepartmentIdAndStatusIn(
            UUID departmentId, Collection<ExpenseStatus> statuses);

    void flush();
}
