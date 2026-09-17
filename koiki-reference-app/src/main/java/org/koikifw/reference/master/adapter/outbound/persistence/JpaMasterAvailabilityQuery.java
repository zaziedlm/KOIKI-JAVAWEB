package org.koikifw.reference.master.adapter.outbound.persistence;

import java.util.Objects;
import java.util.UUID;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.reference.master.contract.MasterAvailabilityQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** JPA-backed implementation of the narrow master availability contract. */
@Repository
@Transactional(readOnly = true)
public class JpaMasterAvailabilityQuery implements MasterAvailabilityQuery {

    private final DepartmentRepository departments;
    private final ExpenseCategoryRepository expenseCategories;
    private final UserDepartmentAssignmentRepository assignments;

    public JpaMasterAvailabilityQuery(
            DepartmentRepository departments,
            ExpenseCategoryRepository expenseCategories,
            UserDepartmentAssignmentRepository assignments) {
        this.departments = departments;
        this.expenseCategories = expenseCategories;
        this.assignments = assignments;
    }

    @Override
    public boolean isActiveDepartment(UUID departmentId) {
        return departments.existsByDepartmentIdAndActiveTrue(
                Objects.requireNonNull(departmentId, "departmentId"));
    }

    @Override
    public boolean isActiveExpenseCategory(UUID expenseCategoryId) {
        return expenseCategories.existsByExpenseCategoryIdAndActiveTrue(
                Objects.requireNonNull(expenseCategoryId, "expenseCategoryId"));
    }

    @Override
    public boolean isUserAssignedToActiveDepartment(
            FrameworkUserId userId, UUID departmentId) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(departmentId, "departmentId");
        return assignments.existsActiveAssignment(userId.value(), departmentId);
    }
}
