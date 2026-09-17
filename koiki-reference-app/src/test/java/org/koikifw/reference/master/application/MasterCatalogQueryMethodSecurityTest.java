package org.koikifw.reference.master.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.koikifw.reference.master.adapter.outbound.persistence.DepartmentRepository;
import org.koikifw.reference.master.adapter.outbound.persistence.ExpenseCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
@Import(MasterCatalogQueryMethodSecurityTest.Configuration.class)
class MasterCatalogQueryMethodSecurityTest {

    @Autowired
    private MasterCatalogQuery catalog;

    @Test
    @WithMockUser(authorities = "EXPENSE:APPLY")
    void deniesMasterListsWithoutMasterAdministrationPermission() {
        assertThatThrownBy(() -> catalog.findDepartments("", 0, 20))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> catalog.findExpenseCategories("", 0, 20))
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
        MasterCatalogQuery masterCatalogQuery(
                DepartmentRepository departments,
                ExpenseCategoryRepository expenseCategories) {
            return new MasterCatalogQuery(departments, expenseCategories);
        }
    }
}
