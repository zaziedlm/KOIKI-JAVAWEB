package org.koikifw.reference.master.adapter.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.koikifw.identity.FrameworkUserId;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JpaMasterAvailabilityQueryTest {

    @Mock
    private DepartmentRepository departments;

    @Mock
    private ExpenseCategoryRepository expenseCategories;

    @Mock
    private UserDepartmentAssignmentRepository assignments;

    @Test
    void delegatesOnlyNarrowAvailabilityChecksToMasterOwnedRepositories() {
        UUID userId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(departments.existsByDepartmentIdAndActiveTrue(departmentId)).thenReturn(true);
        when(expenseCategories.existsByExpenseCategoryIdAndActiveTrue(categoryId)).thenReturn(true);
        when(assignments.existsActiveAssignment(userId, departmentId)).thenReturn(true);
        JpaMasterAvailabilityQuery query =
                new JpaMasterAvailabilityQuery(departments, expenseCategories, assignments);

        assertThat(query.isActiveDepartment(departmentId)).isTrue();
        assertThat(query.isActiveExpenseCategory(categoryId)).isTrue();
        assertThat(query.isUserAssignedToActiveDepartment(
                        FrameworkUserId.parse(userId.toString()), departmentId))
                .isTrue();

        verify(departments).existsByDepartmentIdAndActiveTrue(departmentId);
        verify(expenseCategories).existsByExpenseCategoryIdAndActiveTrue(categoryId);
        verify(assignments).existsActiveAssignment(userId, departmentId);
    }
}
