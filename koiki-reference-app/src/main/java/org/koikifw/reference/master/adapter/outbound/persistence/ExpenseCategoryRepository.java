package org.koikifw.reference.master.adapter.outbound.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Master-owned expense-category persistence boundary. */
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategoryEntity, UUID> {

    boolean existsByExpenseCategoryIdAndActiveTrue(UUID expenseCategoryId);
}
