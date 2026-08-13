package org.koikifw.walkingskeleton.tier2.expense.application;

import java.util.UUID;

import org.koikifw.walkingskeleton.tier2.expense.application.ExpenseDetailView.ExpenseLineView;
import org.koikifw.walkingskeleton.tier2.expense.domain.model.ExpenseRequest;
import org.koikifw.walkingskeleton.tier2.expense.domain.model.ExpenseRequestId;
import org.koikifw.walkingskeleton.tier2.expense.domain.repository.ExpenseRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseDetailQuery {

    private final ExpenseRequestRepository repository;

    public ExpenseDetailQuery(ExpenseRequestRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ExpenseDetailView find(UUID expenseRequestId) {
        ExpenseRequest expenseRequest = repository.findById(ExpenseRequestId.of(expenseRequestId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "expense request not found: " + expenseRequestId));

        return new ExpenseDetailView(
                expenseRequest.id().value(),
                expenseRequest.description(),
                expenseRequest.requestedAmount().value(),
                expenseRequest.status().name(),
                expenseRequest.lines().stream()
                        .map(line -> new ExpenseLineView(
                                line.id(), line.description(), line.amount().value()))
                        .toList());
    }
}
