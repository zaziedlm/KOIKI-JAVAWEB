package org.koikifw.reference.expense.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Child entity whose lifecycle is owned by an {@link ExpenseRequest}. */
@Entity
@Table(name = "kkref_expense_line")
public class ExpenseLine {

    public static final int MAX_DESCRIPTION_LENGTH = 200;
    public static final int MAX_PURPOSE_LENGTH = 500;

    @Id
    @Column(name = "expense_line_id")
    private UUID expenseLineId = UUID.randomUUID();

    @Column(name = "expense_category_id", nullable = false)
    private UUID expenseCategoryId = UUID.randomUUID();

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate = LocalDate.EPOCH;

    @Column(nullable = false, length = MAX_DESCRIPTION_LENGTH)
    private String description = "";

    @Column(nullable = false, length = MAX_PURPOSE_LENGTH)
    private String purpose = "";

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "amount", nullable = false))
    private Money amount = new Money(1);

    protected ExpenseLine() {
    }

    private ExpenseLine(
            UUID expenseLineId,
            UUID expenseCategoryId,
            LocalDate usageDate,
            String description,
            String purpose,
            Money amount,
            LocalDate businessDate) {
        this.expenseLineId = Objects.requireNonNull(expenseLineId, "expenseLineId");
        this.expenseCategoryId = Objects.requireNonNull(expenseCategoryId, "expenseCategoryId");
        this.usageDate = requireUsageDate(usageDate, businessDate);
        this.description = requireText(description, MAX_DESCRIPTION_LENGTH);
        this.purpose = requireText(purpose, MAX_PURPOSE_LENGTH);
        this.amount = Objects.requireNonNull(amount, "amount");
    }

    public static ExpenseLine create(
            UUID expenseLineId,
            UUID expenseCategoryId,
            LocalDate usageDate,
            String description,
            String purpose,
            Money amount,
            LocalDate businessDate) {
        return new ExpenseLine(
                expenseLineId,
                expenseCategoryId,
                usageDate,
                description,
                purpose,
                amount,
                businessDate);
    }

    public UUID expenseLineId() {
        return expenseLineId;
    }

    public UUID expenseCategoryId() {
        return expenseCategoryId;
    }

    public LocalDate usageDate() {
        return usageDate;
    }

    public String description() {
        return description;
    }

    public String purpose() {
        return purpose;
    }

    public Money amount() {
        return amount;
    }

    void revalidate(LocalDate businessDate) {
        requireUsageDate(usageDate, businessDate);
        requireText(description, MAX_DESCRIPTION_LENGTH);
        requireText(purpose, MAX_PURPOSE_LENGTH);
        if (amount.amount() <= 0) {
            throw invalid();
        }
    }

    private static LocalDate requireUsageDate(LocalDate usageDate, LocalDate businessDate) {
        Objects.requireNonNull(usageDate, "usageDate");
        Objects.requireNonNull(businessDate, "businessDate");
        if (usageDate.isAfter(businessDate)) {
            throw invalid();
        }
        return usageDate;
    }

    private static String requireText(String value, int maximumLength) {
        if (value == null) {
            throw invalid();
        }
        String normalized = value.strip();
        if (normalized.isEmpty()
                || normalized.length() > maximumLength
                || normalized.codePoints().anyMatch(Character::isISOControl)) {
            throw invalid();
        }
        return normalized;
    }

    private static ExpenseDomainException invalid() {
        return new ExpenseDomainException(ExpenseRuleViolation.INVALID_CONTENT);
    }
}

