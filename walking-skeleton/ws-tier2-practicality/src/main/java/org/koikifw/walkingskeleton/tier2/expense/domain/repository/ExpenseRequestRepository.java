package org.koikifw.walkingskeleton.tier2.expense.domain.repository;

import java.util.Optional;

import org.koikifw.walkingskeleton.tier2.expense.domain.model.ExpenseRequest;
import org.koikifw.walkingskeleton.tier2.expense.domain.model.ExpenseRequestId;
import org.springframework.data.repository.Repository;

public interface ExpenseRequestRepository
        extends Repository<ExpenseRequest, ExpenseRequestId> {

    ExpenseRequest save(ExpenseRequest expenseRequest);

    Optional<ExpenseRequest> findById(ExpenseRequestId id);
}
