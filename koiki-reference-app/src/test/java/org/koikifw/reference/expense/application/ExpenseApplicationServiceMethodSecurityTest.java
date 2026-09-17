package org.koikifw.reference.expense.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.koikifw.audit.BusinessAuditRecorder;
import org.koikifw.reference.expense.application.port.outbound.ApproverScopePort;
import org.koikifw.reference.expense.application.port.outbound.MasterAvailabilityPort;
import org.koikifw.reference.expense.domain.repository.ExpenseRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
@Import(ExpenseApplicationServiceMethodSecurityTest.Configuration.class)
class ExpenseApplicationServiceMethodSecurityTest {

    private static final UUID ID = UUID.fromString("32000000-0000-0000-0000-000000000001");

    @Autowired
    private ExpenseApplicationService expenses;

    @Test
    @WithMockUser(authorities = "EXPENSE:APPROVE")
    void deniesApplicantOperationsWithoutApplyPermission() {
        assertDenied(() -> expenses.createDraft(ID, 1, List.of()));
        assertDenied(() -> expenses.editDraft(ID, 0, 1, List.of()));
        assertDenied(() -> expenses.submit(ID, 0));
        assertDenied(() -> expenses.beginReedit(ID, 0));
    }

    @Test
    @WithMockUser(authorities = "EXPENSE:APPLY")
    void deniesApprovalOperationsWithoutApprovePermission() {
        assertDenied(() -> expenses.approve(ID, 0));
        assertDenied(() -> expenses.reject(ID, "reason", 0));
        assertDenied(() -> expenses.returnForRework(ID, "reason", 0));
    }

    @Test
    @WithMockUser(authorities = "EXPENSE:APPLY")
    void deniesSettlementWithoutSettlePermission() {
        assertDenied(() -> expenses.settle(ID, 0));
    }

    private static void assertDenied(Runnable operation) {
        assertThatThrownBy(operation::run).isInstanceOf(AccessDeniedException.class);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class Configuration {

        @Bean
        ExpenseRequestRepository expenseRequestRepository() {
            return mock(ExpenseRequestRepository.class);
        }

        @Bean
        MasterAvailabilityPort masterAvailabilityPort() {
            return mock(MasterAvailabilityPort.class);
        }

        @Bean
        ApproverScopePort approverScopePort() {
            return mock(ApproverScopePort.class);
        }

        @Bean
        BusinessAuditRecorder businessAuditRecorder() {
            return mock(BusinessAuditRecorder.class);
        }

        @Bean
        ExpenseApplicationService expenseApplicationService(
                ExpenseRequestRepository requests,
                MasterAvailabilityPort masterAvailability,
                ApproverScopePort approverScope,
                BusinessAuditRecorder audit) {
            return new ExpenseApplicationService(
                    requests, masterAvailability, approverScope, audit);
        }
    }
}
