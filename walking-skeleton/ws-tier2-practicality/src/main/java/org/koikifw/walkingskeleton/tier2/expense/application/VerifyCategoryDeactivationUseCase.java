package org.koikifw.walkingskeleton.tier2.expense.application;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.koikifw.walkingskeleton.tier2.expense.domain.model.ExpenseStatus;
import org.koikifw.walkingskeleton.tier2.expense.domain.repository.ExpenseRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerifyCategoryDeactivationUseCase {

    private static final List<ExpenseStatus> PENDING_STATUSES = List.of(
            ExpenseStatus.DRAFT,
            ExpenseStatus.SUBMITTED);

    private final ExpenseRequestRepository repository;

    public VerifyCategoryDeactivationUseCase(ExpenseRequestRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public void verify(UUID categoryId) {
        Objects.requireNonNull(categoryId, "categoryId must not be null");
        if (repository.existsByCategoryIdAndStatusIn(categoryId, PENDING_STATUSES)) {
            throw new IllegalStateException(
                    "category is referenced by a pending expense: " + categoryId);
        }
    }
}
