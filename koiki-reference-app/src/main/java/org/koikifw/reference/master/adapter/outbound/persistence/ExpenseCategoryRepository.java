package org.koikifw.reference.master.adapter.outbound.persistence;

import java.util.UUID;
import org.koikifw.reference.master.application.dto.ExpenseCategorySummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Master-owned expense-category persistence boundary. */
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategoryEntity, UUID> {

    boolean existsByExpenseCategoryIdAndActiveTrue(UUID expenseCategoryId);

    @Query("""
            select new org.koikifw.reference.master.application.dto.ExpenseCategorySummary(
                category.expenseCategoryId,
                category.expenseCategoryCode,
                category.expenseCategoryName,
                category.active,
                category.version)
            from ReferenceExpenseCategory category
            where :search = ''
               or lower(category.expenseCategoryCode) like concat('%', :search, '%')
               or lower(category.expenseCategoryName) like concat('%', :search, '%')
            order by category.expenseCategoryCode
            """)
    Page<ExpenseCategorySummary> findSummaries(String search, Pageable pageable);
}
