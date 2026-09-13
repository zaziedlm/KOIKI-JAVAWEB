package org.koikifw.reference.expense.application;

import java.util.Objects;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.koikifw.identity.FrameworkPrincipal;
import org.koikifw.identity.FrameworkUserId;
import org.koikifw.reference.expense.application.query.ExpenseRequestPage;
import org.koikifw.reference.expense.application.query.ExpenseRequestDetail;
import org.koikifw.reference.expense.application.query.ExpenseRequestQuery;
import org.koikifw.reference.expense.application.query.ExpenseSelectionOption;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Authorizes expense list use cases and delegates scoped SQL to the query port. */
@Service
public class ExpenseReadService {

    private static final int MAXIMUM_PAGE_SIZE = 100;

    private final ExpenseRequestQuery requests;

    public ExpenseReadService(ExpenseRequestQuery requests) {
        this.requests = Objects.requireNonNull(requests, "requests");
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('EXPENSE:APPLY')")
    public ExpenseRequestPage findOwnRequests(int page, int size) {
        requirePage(page, size);
        return requests.findForApplicant(currentActor(), page, size);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('EXPENSE:APPROVE')")
    public ExpenseRequestPage findApprovalQueue(int page, int size) {
        requirePage(page, size);
        return requests.findForApprover(currentActor(), page, size);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('EXPENSE:SETTLE')")
    public ExpenseRequestPage findAccountingQueue(int page, int size) {
        requirePage(page, size);
        return requests.findForAccounting(page, size);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('EXPENSE:APPLY')")
    public Optional<ExpenseRequestDetail> findOwnRequest(UUID expenseRequestId) {
        return requests.findForApplicantById(currentActor(), requireId(expenseRequestId));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('EXPENSE:APPROVE')")
    public Optional<ExpenseRequestDetail> findApprovalRequest(UUID expenseRequestId) {
        return requests.findForApproverById(currentActor(), requireId(expenseRequestId));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('EXPENSE:SETTLE')")
    public Optional<ExpenseRequestDetail> findAccountingRequest(UUID expenseRequestId) {
        return requests.findForAccountingById(requireId(expenseRequestId));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('EXPENSE:APPLY')")
    public List<ExpenseSelectionOption> findAvailableDepartments() {
        return requests.findAvailableDepartments(currentActor());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('EXPENSE:APPLY')")
    public List<ExpenseSelectionOption> findAvailableExpenseCategories() {
        return requests.findAvailableExpenseCategories();
    }

    private static void requirePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAXIMUM_PAGE_SIZE) {
            throw new ExpenseOperationException(ExpenseFailure.INVALID_INPUT);
        }
    }

    private static UUID requireId(UUID id) {
        if (id == null) {
            throw new ExpenseOperationException(ExpenseFailure.INVALID_INPUT);
        }
        return id;
    }

    private static FrameworkUserId currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof FrameworkPrincipal principal)) {
            throw new ExpenseOperationException(ExpenseFailure.DEPENDENCY_FAILURE);
        }
        return principal.userId();
    }
}
