package org.koikifw.reference.expense.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.koikifw.audit.BusinessAuditRecorder;
import org.koikifw.identity.AuthenticationSource;
import org.koikifw.identity.FrameworkPrincipal;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.reference.expense.application.port.outbound.ApproverScopePort;
import org.koikifw.reference.expense.application.port.outbound.ExpenseApprovedEventPort;
import org.koikifw.reference.expense.application.port.outbound.MasterAvailabilityPort;
import org.koikifw.reference.expense.domain.model.ExpenseLine;
import org.koikifw.reference.expense.domain.model.ExpenseRequest;
import org.koikifw.reference.expense.domain.model.ExpenseStatus;
import org.koikifw.reference.expense.domain.model.Money;
import org.koikifw.reference.expense.domain.repository.ExpenseRequestRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** Checks every scoped mutation without persistence, audit or event side effects. */
class ExpenseMutationAuthorizationOrderTest {
    private static final UUID ID = UUID.randomUUID();
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID DEPARTMENT = UUID.randomUUID();

    private final ExpenseRequestRepository requests = mock(ExpenseRequestRepository.class);
    private final MasterAvailabilityPort master = mock(MasterAvailabilityPort.class);
    private final BusinessAuditRecorder audit = mock(BusinessAuditRecorder.class);
    private final ExpenseApprovedEventPort events = mock(ExpenseApprovedEventPort.class);
    private final ExpenseApplicationService expenses = new ExpenseApplicationService(
            requests, master, mock(ApproverScopePort.class), audit, events);

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @EnumSource(ScopedMutation.class)
    void hidesOutOfScopeAndMissingTargetsForEveryVersionWithoutSideEffects(ScopedMutation operation) {
        var principal = new TestPrincipal(FrameworkUserId.parse(UUID.randomUUID().toString()), Set.of());
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, "unused", List.of()));
        LocalDate date = LocalDate.of(2026, 9, 12);
        var request = ExpenseRequest.createDraft(ID, OWNER, DEPARTMENT, new Money(100),
                List.of(ExpenseLine.create(UUID.randomUUID(), UUID.randomUUID(), date,
                        "Taxi", "Visit", new Money(100), date)), date, Instant.EPOCH);

        for (long version : new long[] {0, 1, -1}) {
            when(requests.findById(ID)).thenReturn(Optional.of(request));
            assertNotFound(() -> operation.invoke(expenses, ID, version));
            when(requests.findById(ID)).thenReturn(Optional.empty());
            assertNotFound(() -> operation.invoke(expenses, ID, version));
            assertThat(request.status()).isEqualTo(ExpenseStatus.DRAFT);
            assertThat(request.version()).isZero();
            assertThat(request.claimedAmount().amount()).isEqualTo(100);
            assertThat(request.lines()).hasSize(1);
            verify(requests, never()).flush();
            verifyNoInteractions(master, audit, events);
        }
    }

    private static void assertNotFound(Runnable operation) {
        assertThatThrownBy(operation::run).isInstanceOf(ExpenseOperationException.class)
                .extracting(exception -> ((ExpenseOperationException) exception).failure())
                .isEqualTo(ExpenseFailure.NOT_FOUND);
    }

    enum ScopedMutation {
        EDIT, SUBMIT, BEGIN_REEDIT, APPROVE, REJECT, RETURN;

        void invoke(ExpenseApplicationService service, UUID id, long version) {
            switch (this) {
                case EDIT -> service.editDraft(id, version, 100, List.of());
                case SUBMIT -> service.submit(id, version);
                case BEGIN_REEDIT -> service.beginReedit(id, version);
                case APPROVE -> service.approve(id, version);
                case REJECT -> service.reject(id, "reason", version);
                case RETURN -> service.returnForRework(id, "reason", version);
            }
        }
    }

    private record TestPrincipal(FrameworkUserId userId, Set<String> permissions) implements FrameworkPrincipal {
        @Override
        public AuthenticationSource authenticationSource() {
            return AuthenticationSource.LOCAL;
        }
    }
}
