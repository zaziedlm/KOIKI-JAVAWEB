package org.koikifw.reference.expense.adapter.inbound.api;

import java.util.List;
import java.util.UUID;
import org.koikifw.reference.expense.application.ExpenseLineInput;
import org.koikifw.reference.expense.application.query.ExpenseRequestDetail;
import org.koikifw.reference.expense.application.query.ExpenseRequestLineView;
import org.springframework.stereotype.Component;

/** Copies transport-neutral application values into the approved REST contract. */
@Component
final class ExpenseApiMapper {

    List<ExpenseLineInput> toLineInputs(CreateExpenseRequest request) {
        return request.lines().stream()
                .map(line -> new ExpenseLineInput(
                        UUID.randomUUID(),
                        line.expenseCategoryId(),
                        line.usageDate(),
                        line.description(),
                        line.purpose(),
                        line.amount()))
                .toList();
    }

    ExpenseDetailResponse toResponse(ExpenseRequestDetail detail) {
        return new ExpenseDetailResponse(
                detail.expenseRequestId(),
                new ExpenseDepartmentResponse(
                        detail.departmentId(), detail.departmentCode(), detail.departmentName()),
                detail.claimedAmount(),
                detail.status(),
                detail.decisionReason(),
                detail.version(),
                detail.updatedAt(),
                detail.lines().stream().map(ExpenseApiMapper::toResponse).toList());
    }

    private static ExpenseLineResponse toResponse(ExpenseRequestLineView line) {
        return new ExpenseLineResponse(
                line.expenseLineId(),
                line.expenseCategoryId(),
                line.expenseCategoryCode(),
                line.expenseCategoryName(),
                line.usageDate(),
                line.description(),
                line.purpose(),
                line.amount());
    }
}
