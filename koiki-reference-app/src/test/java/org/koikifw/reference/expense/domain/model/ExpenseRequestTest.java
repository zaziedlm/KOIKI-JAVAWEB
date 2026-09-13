package org.koikifw.reference.expense.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExpenseRequestTest {

    private static final UUID APPLICANT = UUID.fromString(
            "30000000-0000-0000-0000-000000000001");
    private static final UUID APPROVER = UUID.fromString(
            "30000000-0000-0000-0000-000000000002");
    private static final UUID DEPARTMENT = UUID.fromString(
            "30000000-0000-0000-0000-000000000003");
    private static final UUID CATEGORY = UUID.fromString(
            "30000000-0000-0000-0000-000000000004");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 13);
    private static final Instant NOW = Instant.parse("2026-09-13T00:00:00Z");

    @Test
    void followsAllSevenAcceptedTransitions() {
        ExpenseRequest approved = draft();
        approved.submit(TODAY, NOW);
        approved.approve(APPROVER, NOW);
        approved.completeSettlement(NOW);
        assertThat(approved.status()).isEqualTo(ExpenseStatus.SETTLED);

        ExpenseRequest rejected = draft();
        rejected.submit(TODAY, NOW);
        rejected.reject(APPROVER, "  duplicate  ", NOW);
        assertThat(rejected.status()).isEqualTo(ExpenseStatus.REJECTED);
        assertThat(rejected.decisionReason()).isEqualTo("duplicate");

        ExpenseRequest returned = draft();
        returned.submit(TODAY, NOW);
        returned.returnForRework(APPROVER, "  clarify purpose  ", NOW);
        returned.beginReedit(NOW);
        returned.edit(new Money(200), List.of(line(200)), TODAY, NOW);
        returned.submit(TODAY, NOW);
        assertThat(returned.status()).isEqualTo(ExpenseStatus.SUBMITTED);
        assertThat(returned.departmentId()).isEqualTo(DEPARTMENT);
    }

    @Test
    void rejectsAmountMismatchWithoutChangingDraft() {
        ExpenseRequest request = draft();

        assertThatThrownBy(() -> request.edit(
                        new Money(999), List.of(line(100)), TODAY, NOW))
                .isInstanceOf(ExpenseDomainException.class)
                .extracting(exception -> ((ExpenseDomainException) exception).violation())
                .isEqualTo(ExpenseRuleViolation.INVALID_CONTENT);

        assertThat(request.claimedAmount()).isEqualTo(new Money(100));
        assertThat(request.lines()).hasSize(1);
        assertThat(request.status()).isEqualTo(ExpenseStatus.DRAFT);
    }

    @Test
    void rejectsEditingOutsideDraftSelfDecisionAndUnlistedTransitions() {
        ExpenseRequest request = draft();
        request.submit(TODAY, NOW);

        assertViolation(
                () -> request.edit(new Money(100), List.of(line(100)), TODAY, NOW),
                ExpenseRuleViolation.INVALID_TRANSITION);
        assertViolation(
                () -> request.approve(APPLICANT, NOW), ExpenseRuleViolation.SELF_DECISION);
        assertViolation(
                () -> request.completeSettlement(NOW),
                ExpenseRuleViolation.INVALID_TRANSITION);
        assertThat(request.status()).isEqualTo(ExpenseStatus.SUBMITTED);
    }

    @Test
    void rejectsFutureUsageEmptyReasonsAndIncompleteDrafts() {
        assertViolation(
                () -> ExpenseLine.create(
                        UUID.randomUUID(),
                        CATEGORY,
                        TODAY.plusDays(1),
                        "Taxi",
                        "Client visit",
                        new Money(100),
                        TODAY),
                ExpenseRuleViolation.INVALID_CONTENT);
        assertViolation(
                () -> ExpenseRequest.createDraft(
                        UUID.randomUUID(),
                        APPLICANT,
                        DEPARTMENT,
                        new Money(100),
                        List.of(),
                        TODAY,
                        NOW),
                ExpenseRuleViolation.INVALID_CONTENT);

        ExpenseRequest request = draft();
        request.submit(TODAY, NOW);
        assertViolation(
                () -> request.reject(APPROVER, "  ", NOW),
                ExpenseRuleViolation.INVALID_CONTENT);
        assertThat(request.status()).isEqualTo(ExpenseStatus.SUBMITTED);
    }

    private static ExpenseRequest draft() {
        return ExpenseRequest.createDraft(
                UUID.randomUUID(),
                APPLICANT,
                DEPARTMENT,
                new Money(100),
                List.of(line(100)),
                TODAY,
                NOW);
    }

    private static ExpenseLine line(long amount) {
        return ExpenseLine.create(
                UUID.randomUUID(),
                CATEGORY,
                TODAY,
                "Taxi",
                "Client visit",
                new Money(amount),
                TODAY);
    }

    private static void assertViolation(Runnable operation, ExpenseRuleViolation violation) {
        assertThatThrownBy(operation::run)
                .isInstanceOf(ExpenseDomainException.class)
                .extracting(exception -> ((ExpenseDomainException) exception).violation())
                .isEqualTo(violation);
    }
}

