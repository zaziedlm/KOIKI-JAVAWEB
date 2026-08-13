package org.koikifw.walkingskeleton.tier2.expense.domain.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "ws_expense_request")
public class ExpenseRequest implements Persistable<ExpenseRequestId> {

    @EmbeddedId
    private ExpenseRequestId id;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(
            name = "requested_amount", precision = 19, scale = 2, nullable = false))
    private Money requestedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExpenseStatus status;

    @OneToMany(
            mappedBy = "expenseRequest",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("lineOrder ASC")
    private List<ExpenseLine> lines = new ArrayList<>();

    @Transient
    private boolean newAggregate = true;

    protected ExpenseRequest() {
        // JPA only
    }

    private ExpenseRequest(
            UUID categoryId,
            String description,
            Money requestedAmount,
            List<ExpenseLine> lines) {
        this.id = ExpenseRequestId.newId();
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId must not be null");
        this.description = requireDescription(description);
        this.requestedAmount = Objects.requireNonNull(
                requestedAmount, "requestedAmount must not be null");
        this.status = ExpenseStatus.DRAFT;

        Objects.requireNonNull(lines, "lines must not be null");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("at least one expense line is required");
        }
        for (int index = 0; index < lines.size(); index++) {
            ExpenseLine line = Objects.requireNonNull(
                    lines.get(index), "expense line must not be null");
            line.attachTo(this, index);
            this.lines.add(line);
        }
    }

    public static ExpenseRequest draft(
            UUID categoryId,
            String description,
            Money requestedAmount,
            List<ExpenseLine> lines) {
        return new ExpenseRequest(categoryId, description, requestedAmount, List.copyOf(lines));
    }

    public void submit() {
        requireStatus(ExpenseStatus.DRAFT, "submit");
        BigDecimal lineTotal = lines.stream()
                .map(line -> line.amount().value())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!requestedAmount.hasSameValueAs(lineTotal)) {
            throw new IllegalStateException(
                    "expense line total must equal requested amount");
        }
        status = ExpenseStatus.SUBMITTED;
    }

    public void approve() {
        requireStatus(ExpenseStatus.SUBMITTED, "approve");
        status = ExpenseStatus.APPROVED;
    }

    public void reject() {
        requireStatus(ExpenseStatus.SUBMITTED, "reject");
        status = ExpenseStatus.REJECTED;
    }

    public ExpenseRequestId id() {
        return id;
    }

    @Override
    public ExpenseRequestId getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return newAggregate;
    }

    public UUID categoryId() {
        return categoryId;
    }

    public String description() {
        return description;
    }

    public Money requestedAmount() {
        return requestedAmount;
    }

    public ExpenseStatus status() {
        return status;
    }

    public List<ExpenseLine> lines() {
        return Collections.unmodifiableList(lines);
    }

    @PostPersist
    @PostLoad
    void markPersisted() {
        newAggregate = false;
    }

    private void requireStatus(ExpenseStatus expected, String operation) {
        if (status != expected) {
            throw new IllegalStateException(
                    "cannot " + operation + " expense request in " + status + " status");
        }
    }

    private static String requireDescription(String description) {
        Objects.requireNonNull(description, "description must not be null");
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        return description;
    }
}
