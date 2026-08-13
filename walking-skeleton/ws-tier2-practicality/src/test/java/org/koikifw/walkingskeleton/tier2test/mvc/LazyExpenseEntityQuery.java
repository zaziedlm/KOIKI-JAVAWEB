package org.koikifw.walkingskeleton.tier2test.mvc;

import java.util.UUID;

import org.koikifw.walkingskeleton.tier2.expense.domain.model.ExpenseRequest;
import org.koikifw.walkingskeleton.tier2.expense.domain.model.ExpenseRequestId;
import org.koikifw.walkingskeleton.tier2.expense.domain.repository.ExpenseRequestRepository;
import org.springframework.transaction.annotation.Transactional;

public class LazyExpenseEntityQuery {

    private final ExpenseRequestRepository repository;

    public LazyExpenseEntityQuery(ExpenseRequestRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ExpenseRequest find(UUID expenseRequestId) {
        return repository.findById(ExpenseRequestId.of(expenseRequestId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "expense request not found: " + expenseRequestId));
    }
}
