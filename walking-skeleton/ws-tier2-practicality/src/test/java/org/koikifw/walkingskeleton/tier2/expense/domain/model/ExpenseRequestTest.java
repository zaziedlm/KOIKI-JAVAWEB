package org.koikifw.walkingskeleton.tier2.expense.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ExpenseRequestTest {

    private static final UUID CATEGORY_ID = UUID.fromString(
            "10000000-0000-0000-0000-000000000001");

    @Test
    void createsDraftWithPositiveAmountsAndAtLeastOneLine() {
        ExpenseRequest request = draft(
                "1200.00",
                ExpenseLine.of("train", money("800.00")),
                ExpenseLine.of("bus", money("400.00")));

        assertThat(request.id().value()).isNotNull();
        assertThat(request.status()).isEqualTo(ExpenseStatus.DRAFT);
        assertThat(request.lines())
                .extracting(ExpenseLine::lineOrder)
                .containsExactly(0, 1);
    }

    @Test
    void rejectsNonPositiveAmountsAndMissingLines() {
        assertThatThrownBy(() -> Money.of(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("amount must be positive");
        assertThatThrownBy(() -> Money.of(new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("amount must be positive");
        assertThatThrownBy(() -> ExpenseRequest.draft(
                CATEGORY_ID, "business trip", money("100.00"), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("at least one expense line is required");
    }

    @Test
    void submitsOnlyWhenLineTotalMatchesRequestedAmount() {
        ExpenseRequest matching = draft(
                "1200.00",
                ExpenseLine.of("train", money("800.00")),
                ExpenseLine.of("bus", money("400.0")));
        matching.submit();
        assertThat(matching.status()).isEqualTo(ExpenseStatus.SUBMITTED);

        ExpenseRequest mismatching = draft(
                "1200.00", ExpenseLine.of("train", money("800.00")));
        assertThatThrownBy(mismatching::submit)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("expense line total must equal requested amount");
        assertThat(mismatching.status()).isEqualTo(ExpenseStatus.DRAFT);
    }

    @Test
    void permitsOnlyDraftSubmittedApprovedOrRejectedTransitions() {
        ExpenseRequest approved = draft(
                "800.00", ExpenseLine.of("train", money("800.00")));
        assertThatThrownBy(approved::approve)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT status");
        assertThatThrownBy(approved::reject)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT status");

        approved.submit();
        assertThatThrownBy(approved::submit)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SUBMITTED status");
        approved.approve();
        assertThat(approved.status()).isEqualTo(ExpenseStatus.APPROVED);
        assertThatThrownBy(approved::reject)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APPROVED status");

        ExpenseRequest rejected = draft(
                "400.00", ExpenseLine.of("bus", money("400.00")));
        rejected.submit();
        rejected.reject();
        assertThat(rejected.status()).isEqualTo(ExpenseStatus.REJECTED);
    }

    private static ExpenseRequest draft(String requestedAmount, ExpenseLine... lines) {
        return ExpenseRequest.draft(
                CATEGORY_ID,
                "business trip",
                money(requestedAmount),
                List.of(lines));
    }

    private static Money money(String value) {
        return Money.of(new BigDecimal(value));
    }
}
