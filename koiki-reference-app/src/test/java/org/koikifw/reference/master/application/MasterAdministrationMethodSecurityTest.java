package org.koikifw.reference.master.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.koikifw.audit.BusinessAuditRecorder;
import org.koikifw.reference.master.adapter.outbound.persistence.DepartmentRepository;
import org.koikifw.reference.master.adapter.outbound.persistence.ExpenseCategoryRepository;
import org.koikifw.reference.master.adapter.outbound.persistence.UserDepartmentAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.mockito.Mockito.mock;

@SpringJUnitConfig
@Import(MasterAdministrationMethodSecurityTest.Configuration.class)
class MasterAdministrationMethodSecurityTest {

    @Autowired
    private MasterAdministration administration;

    @Test
    @WithMockUser(authorities = "EXPENSE:APPLY")
    void deniesCallerWithoutMasterAdministrationPermission() {
        assertThatThrownBy(() -> administration.createDepartment("FINANCE", "Finance"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class Configuration {

        @Bean
        DepartmentRepository departmentRepository() {
            return mock(DepartmentRepository.class);
        }

        @Bean
        ExpenseCategoryRepository expenseCategoryRepository() {
            return mock(ExpenseCategoryRepository.class);
        }

        @Bean
        UserDepartmentAssignmentRepository userDepartmentAssignmentRepository() {
            return mock(UserDepartmentAssignmentRepository.class);
        }

        @Bean
        BusinessAuditRecorder businessAuditRecorder() {
            return mock(BusinessAuditRecorder.class);
        }

        @Bean
        MasterAdministration masterAdministration(
                DepartmentRepository departments,
                ExpenseCategoryRepository expenseCategories,
                UserDepartmentAssignmentRepository assignments,
                BusinessAuditRecorder audit) {
            return new MasterAdministration(departments, expenseCategories, assignments, audit);
        }
    }
}
