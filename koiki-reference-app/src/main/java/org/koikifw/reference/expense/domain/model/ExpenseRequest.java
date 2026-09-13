package org.koikifw.reference.expense.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Tier 2 aggregate that owns expense content invariants and lifecycle transitions. */
@Entity
@Table(name = "kkref_expense_request")
public class ExpenseRequest {

    public static final int MAX_REASON_LENGTH = 500;

    @Id
    @Column(name = "expense_request_id")
    private UUID expenseRequestId = UUID.randomUUID();

    @Column(name = "applicant_user_id", nullable = false)
    private UUID applicantUserId = UUID.randomUUID();

    @Column(name = "department_id", nullable = false)
    private UUID departmentId = UUID.randomUUID();

    @Embedded
    @AttributeOverride(
            name = "amount",
            column = @Column(name = "claimed_amount", nullable = false))
    private Money claimedAmount = new Money(1);

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "expense_request_id", nullable = false)
    private List<ExpenseLine> lines = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseStatus status = ExpenseStatus.DRAFT;

    @Column(name = "decision_reason", length = MAX_REASON_LENGTH)
    private @Nullable String decisionReason;

    @Version
    private @Nullable Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.EPOCH;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.EPOCH;

    protected ExpenseRequest() {
    }

    private ExpenseRequest(
            UUID expenseRequestId,
            UUID applicantUserId,
            UUID departmentId,
            Money claimedAmount,
            List<ExpenseLine> lines,
            LocalDate businessDate,
            Instant now) {
        this.expenseRequestId = Objects.requireNonNull(expenseRequestId, "expenseRequestId");
        this.applicantUserId = Objects.requireNonNull(applicantUserId, "applicantUserId");
        this.departmentId = Objects.requireNonNull(departmentId, "departmentId");
        ValidatedContent content = validate(claimedAmount, lines, businessDate);
        this.claimedAmount = content.claimedAmount();
        this.lines = new ArrayList<>(content.lines());
        this.createdAt = Objects.requireNonNull(now, "now");
        this.updatedAt = now;
    }

    public static ExpenseRequest createDraft(
            UUID expenseRequestId,
            UUID applicantUserId,
            UUID departmentId,
            Money claimedAmount,
            List<ExpenseLine> lines,
            LocalDate businessDate,
            Instant now) {
        return new ExpenseRequest(
                expenseRequestId,
                applicantUserId,
                departmentId,
                claimedAmount,
                lines,
                businessDate,
                now);
    }

    public void edit(
            Money newClaimedAmount,
            List<ExpenseLine> newLines,
            LocalDate businessDate,
            Instant now) {
        requireStatus(ExpenseStatus.DRAFT);
        ValidatedContent content = validate(newClaimedAmount, newLines, businessDate);
        claimedAmount = content.claimedAmount();
        lines.clear();
        lines.addAll(content.lines());
        touch(now);
    }

    public void submit(LocalDate businessDate, Instant now) {
        requireStatus(ExpenseStatus.DRAFT);
        validate(claimedAmount, lines, businessDate);
        decisionReason = null;
        transitionTo(ExpenseStatus.SUBMITTED, now);
    }

    public void approve(UUID actorUserId, Instant now) {
        requireDecisionActor(actorUserId);
        requireStatus(ExpenseStatus.SUBMITTED);
        decisionReason = null;
        transitionTo(ExpenseStatus.APPROVED, now);
    }

    public void reject(UUID actorUserId, String reason, Instant now) {
        requireDecisionActor(actorUserId);
        requireStatus(ExpenseStatus.SUBMITTED);
        decisionReason = requireReason(reason);
        transitionTo(ExpenseStatus.REJECTED, now);
    }

    public void returnForRework(UUID actorUserId, String reason, Instant now) {
        requireDecisionActor(actorUserId);
        requireStatus(ExpenseStatus.SUBMITTED);
        decisionReason = requireReason(reason);
        transitionTo(ExpenseStatus.RETURNED, now);
    }

    public void beginReedit(Instant now) {
        requireStatus(ExpenseStatus.RETURNED);
        transitionTo(ExpenseStatus.DRAFT, now);
    }

    public void completeSettlement(Instant now) {
        requireStatus(ExpenseStatus.APPROVED);
        transitionTo(ExpenseStatus.SETTLED, now);
    }

    public UUID expenseRequestId() {
        return expenseRequestId;
    }

    public UUID applicantUserId() {
        return applicantUserId;
    }

    public UUID departmentId() {
        return departmentId;
    }

    public Money claimedAmount() {
        return claimedAmount;
    }

    public List<ExpenseLine> lines() {
        return List.copyOf(lines);
    }

    public ExpenseStatus status() {
        return status;
    }

    public @Nullable String decisionReason() {
        return decisionReason;
    }

    public long version() {
        return version == null ? 0 : version;
    }

    private void requireDecisionActor(UUID actorUserId) {
        Objects.requireNonNull(actorUserId, "actorUserId");
        if (applicantUserId.equals(actorUserId)) {
            throw new ExpenseDomainException(ExpenseRuleViolation.SELF_DECISION);
        }
    }

    private void requireStatus(ExpenseStatus required) {
        if (status != required) {
            throw new ExpenseDomainException(ExpenseRuleViolation.INVALID_TRANSITION);
        }
    }

    private void transitionTo(ExpenseStatus next, Instant now) {
        status = next;
        touch(now);
    }

    private void touch(Instant now) {
        updatedAt = Objects.requireNonNull(now, "now");
    }

    private static ValidatedContent validate(
            Money claimedAmount,
            List<ExpenseLine> lines,
            LocalDate businessDate) {
        Objects.requireNonNull(claimedAmount, "claimedAmount");
        Objects.requireNonNull(lines, "lines");
        Objects.requireNonNull(businessDate, "businessDate");
        if (lines.isEmpty() || lines.stream().anyMatch(Objects::isNull)) {
            throw new ExpenseDomainException(ExpenseRuleViolation.INVALID_CONTENT);
        }
        List<ExpenseLine> copy = List.copyOf(lines);
        copy.forEach(line -> line.revalidate(businessDate));
        if (!claimedAmount.equals(Money.sum(copy))) {
            throw new ExpenseDomainException(ExpenseRuleViolation.INVALID_CONTENT);
        }
        return new ValidatedContent(claimedAmount, copy);
    }

    private static String requireReason(String reason) {
        if (reason == null) {
            throw new ExpenseDomainException(ExpenseRuleViolation.INVALID_CONTENT);
        }
        String normalized = reason.strip();
        if (normalized.isEmpty()
                || normalized.length() > MAX_REASON_LENGTH
                || normalized.codePoints().anyMatch(Character::isISOControl)) {
            throw new ExpenseDomainException(ExpenseRuleViolation.INVALID_CONTENT);
        }
        return normalized;
    }

    private record ValidatedContent(Money claimedAmount, List<ExpenseLine> lines) {
    }
}
