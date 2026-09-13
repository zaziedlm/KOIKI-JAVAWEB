package org.koikifw.reference.expense.adapter.inbound.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.koikifw.reference.expense.application.ExpenseLineInput;
import org.koikifw.reference.expense.application.query.ExpenseRequestDetail;
import org.koikifw.reference.expense.application.query.ExpenseRequestLineView;

/** One-line minimal expense Form; the aggregate use case still validates all invariants. */
record ExpenseDraftForm(
        @Nullable @NotBlank @Pattern(regexp = ExpenseDraftForm.UUID_PATTERN) String departmentId,
        @Nullable @Positive Long claimedAmount,
        @Nullable @Pattern(regexp = ExpenseDraftForm.UUID_PATTERN) String expenseLineId,
        @Nullable @NotBlank @Pattern(regexp = ExpenseDraftForm.UUID_PATTERN) String expenseCategoryId,
        @Nullable @PastOrPresent LocalDate usageDate,
        @Nullable @NotBlank @Size(max = 200) String description,
        @Nullable @NotBlank @Size(max = 500) String purpose,
        @Nullable @Positive Long lineAmount,
        @Nullable Long expectedVersion) {

    static final String UUID_PATTERN =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";

    static ExpenseDraftForm empty() {
        return new ExpenseDraftForm(null, null, null, null, null, null, null, null, null);
    }

    static ExpenseDraftForm from(ExpenseRequestDetail detail) {
        ExpenseRequestLineView line = detail.lines().getFirst();
        return new ExpenseDraftForm(
                detail.departmentId().toString(),
                detail.claimedAmount(),
                line.expenseLineId().toString(),
                line.expenseCategoryId().toString(),
                line.usageDate(),
                line.description(),
                line.purpose(),
                line.amount(),
                detail.version());
    }

    boolean completeForCreate() {
        return departmentId != null && claimedAmount != null && expenseCategoryId != null
                && usageDate != null && description != null && purpose != null && lineAmount != null;
    }

    boolean completeForEdit() {
        return completeForCreate() && expenseLineId != null && expectedVersion != null;
    }

    UUID requiredDepartmentId() {
        return UUID.fromString(java.util.Objects.requireNonNull(departmentId));
    }

    long requiredClaimedAmount() {
        return java.util.Objects.requireNonNull(claimedAmount);
    }

    long requiredExpectedVersion() {
        return java.util.Objects.requireNonNull(expectedVersion);
    }

    ExpenseLineInput toLineInput() {
        UUID lineId = expenseLineId == null ? UUID.randomUUID() : UUID.fromString(expenseLineId);
        return new ExpenseLineInput(
                lineId,
                UUID.fromString(java.util.Objects.requireNonNull(expenseCategoryId)),
                java.util.Objects.requireNonNull(usageDate),
                java.util.Objects.requireNonNull(description),
                java.util.Objects.requireNonNull(purpose),
                java.util.Objects.requireNonNull(lineAmount));
    }
}
