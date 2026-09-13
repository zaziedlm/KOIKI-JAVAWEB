package org.koikifw.reference.master.application;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.koikifw.reference.master.adapter.outbound.persistence.DepartmentRepository;
import org.koikifw.reference.master.adapter.outbound.persistence.ExpenseCategoryRepository;
import org.koikifw.reference.master.application.dto.DepartmentPage;
import org.koikifw.reference.master.application.dto.ExpenseCategoryPage;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads Tier 1 master lists through JPA class-based projections. */
@Service
@PreAuthorize("hasAuthority('MASTER:ADMIN')")
public class MasterCatalogQuery {

    private static final int MAXIMUM_PAGE_SIZE = 100;

    private final DepartmentRepository departments;
    private final ExpenseCategoryRepository expenseCategories;

    public MasterCatalogQuery(
            DepartmentRepository departments,
            ExpenseCategoryRepository expenseCategories) {
        this.departments = Objects.requireNonNull(departments, "departments");
        this.expenseCategories = Objects.requireNonNull(expenseCategories, "expenseCategories");
    }

    @Transactional(readOnly = true)
    public DepartmentPage findDepartments(@Nullable String search, int page, int size) {
        PageRequest request = pageRequest(page, size);
        var result = departments.findSummaries(normalize(search), request);
        return new DepartmentPage(result.getContent(), result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public ExpenseCategoryPage findExpenseCategories(@Nullable String search, int page, int size) {
        PageRequest request = pageRequest(page, size);
        var result = expenseCategories.findSummaries(normalize(search), request);
        return new ExpenseCategoryPage(result.getContent(), result.getTotalElements(), page, size);
    }

    private static PageRequest pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > MAXIMUM_PAGE_SIZE) {
            throw new MasterOperationException(MasterFailure.INVALID_INPUT);
        }
        return PageRequest.of(page, size);
    }

    private static String normalize(@Nullable String search) {
        return search == null ? "" : search.strip().toLowerCase(java.util.Locale.ROOT);
    }
}
