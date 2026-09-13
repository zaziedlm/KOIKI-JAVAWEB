package org.koikifw.reference.expense.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.koikifw.reference.expense.application.query.ExpenseRequestQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
@Import(ExpenseReadServiceMethodSecurityTest.Configuration.class)
class ExpenseReadServiceMethodSecurityTest {

    @Autowired
    private ExpenseReadService reads;

    @Test
    @WithMockUser(authorities = "MASTER:ADMIN")
    void deniesEveryExpenseListWithoutItsPermission() {
        assertThatThrownBy(() -> reads.findOwnRequests(0, 20))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> reads.findApprovalQueue(0, 20))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> reads.findAccountingQueue(0, 20))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> reads.findOwnRequest(java.util.UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> reads.findApprovalRequest(java.util.UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> reads.findAccountingRequest(java.util.UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(reads::findAvailableDepartments)
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(reads::findAvailableExpenseCategories)
                .isInstanceOf(AccessDeniedException.class);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class Configuration {

        @Bean
        ExpenseRequestQuery expenseRequestQuery() {
            return mock(ExpenseRequestQuery.class);
        }

        @Bean
        ExpenseReadService expenseReadService(ExpenseRequestQuery requests) {
            return new ExpenseReadService(requests);
        }
    }
}
