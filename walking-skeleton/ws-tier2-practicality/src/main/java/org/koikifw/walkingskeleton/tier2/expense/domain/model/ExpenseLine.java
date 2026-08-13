package org.koikifw.walkingskeleton.tier2.expense.domain.model;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ws_expense_line")
public class ExpenseLine {

    @Id
    @Column(name = "expense_line_id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_request_id", nullable = false, updatable = false)
    private ExpenseRequest expenseRequest;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(
            name = "amount", precision = 19, scale = 2, nullable = false))
    private Money amount;

    @Column(name = "line_order", nullable = false)
    private int lineOrder;

    protected ExpenseLine() {
        // JPA only
    }

    private ExpenseLine(String description, Money amount) {
        this.id = UUID.randomUUID();
        this.description = requireDescription(description);
        this.amount = Objects.requireNonNull(amount, "amount must not be null");
    }

    public static ExpenseLine of(String description, Money amount) {
        return new ExpenseLine(description, amount);
    }

    void attachTo(ExpenseRequest expenseRequest, int lineOrder) {
        if (this.expenseRequest != null) {
            throw new IllegalStateException("expense line is already attached");
        }
        this.expenseRequest = Objects.requireNonNull(
                expenseRequest, "expenseRequest must not be null");
        this.lineOrder = lineOrder;
    }

    public UUID id() {
        return id;
    }

    public String description() {
        return description;
    }

    public Money amount() {
        return amount;
    }

    public int lineOrder() {
        return lineOrder;
    }

    private static String requireDescription(String description) {
        Objects.requireNonNull(description, "description must not be null");
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        return description;
    }
}
