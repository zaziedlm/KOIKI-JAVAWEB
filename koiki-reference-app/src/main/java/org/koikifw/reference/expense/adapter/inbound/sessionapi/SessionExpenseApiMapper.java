package org.koikifw.reference.expense.adapter.inbound.sessionapi;

import java.util.List;
import java.util.UUID;
import org.koikifw.reference.expense.application.ExpenseLineInput;
import org.koikifw.reference.expense.application.query.ExpenseRequestDetail;
import org.koikifw.reference.expense.application.query.ExpenseRequestPage;
import org.koikifw.reference.expense.application.query.ExpenseSelectionOption;
import org.springframework.stereotype.Component;

/** Copies only applicant-safe application values, preserving long precision as decimal strings. */
@Component
final class SessionExpenseApiMapper {
    List<ExpenseLineInput> toLines(CreateSessionExpenseRequest request) {
        return request.lines().stream().map(line -> new ExpenseLineInput(UUID.randomUUID(),
                line.expenseCategoryId(), line.usageDate(), line.description(), line.purpose(),
                Long.parseLong(line.amount()))).toList();
    }

    SessionExpenseOptionResponse option(ExpenseSelectionOption value) {
        return new SessionExpenseOptionResponse(value.id(), value.code(), value.name());
    }

    SessionExpensePageResponse page(ExpenseRequestPage value) {
        return new SessionExpensePageResponse(value.content().stream().map(row ->
                new SessionExpenseListItemResponse(row.expenseRequestId(),
                        new SessionExpenseOptionResponse(row.departmentId(), row.departmentCode(), row.departmentName()),
                        Long.toString(row.claimedAmount()), row.status(), Long.toString(row.version()), row.updatedAt()))
                .toList(), Long.toString(value.totalElements()), value.page(), value.size());
    }

    SessionExpenseDetailResponse detail(ExpenseRequestDetail value) {
        return new SessionExpenseDetailResponse(value.expenseRequestId(),
                new SessionExpenseOptionResponse(value.departmentId(), value.departmentCode(), value.departmentName()),
                Long.toString(value.claimedAmount()), value.status(), value.decisionReason(),
                Long.toString(value.version()), value.updatedAt(), value.lines().stream().map(line ->
                        new SessionExpenseLineResponse(line.expenseLineId(), new SessionExpenseOptionResponse(
                                line.expenseCategoryId(), line.expenseCategoryCode(), line.expenseCategoryName()),
                                line.usageDate(), line.description(), line.purpose(), Long.toString(line.amount())))
                        .toList());
    }
}
