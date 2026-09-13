package org.koikifw.reference.expense.application.query;

import org.koikifw.identity.FrameworkUserId;

/** Expense-owned query port whose implementations enforce actor scope in SQL. */
public interface ExpenseRequestQuery {

    ExpenseRequestPage findForApplicant(FrameworkUserId applicantUserId, int page, int size);

    ExpenseRequestPage findForApprover(FrameworkUserId approverUserId, int page, int size);

    ExpenseRequestPage findForAccounting(int page, int size);
}
