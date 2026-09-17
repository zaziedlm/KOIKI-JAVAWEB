package org.koikifw.reference.expense.application.query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.koikifw.identity.FrameworkUserId;

/** Expense-owned query port whose implementations enforce actor scope in SQL. */
public interface ExpenseRequestQuery {

    ExpenseRequestPage findForApplicant(FrameworkUserId applicantUserId, int page, int size);

    ExpenseRequestPage findForApprover(FrameworkUserId approverUserId, int page, int size);

    ExpenseRequestPage findForAccounting(int page, int size);

    Optional<ExpenseRequestDetail> findForApplicantById(
            FrameworkUserId applicantUserId, UUID expenseRequestId);

    Optional<ExpenseRequestDetail> findForApproverById(
            FrameworkUserId approverUserId, UUID expenseRequestId);

    Optional<ExpenseRequestDetail> findForAccountingById(UUID expenseRequestId);

    List<ExpenseSelectionOption> findAvailableDepartments(FrameworkUserId applicantUserId);

    List<ExpenseSelectionOption> findAvailableExpenseCategories();
}
