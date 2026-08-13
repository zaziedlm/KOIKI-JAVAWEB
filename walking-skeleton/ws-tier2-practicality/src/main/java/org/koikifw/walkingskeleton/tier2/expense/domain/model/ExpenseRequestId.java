package org.koikifw.walkingskeleton.tier2.expense.domain.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public final class ExpenseRequestId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "expense_request_id", nullable = false, updatable = false)
    private UUID value;

    ExpenseRequestId() {
        // JPA only
    }

    private ExpenseRequestId(UUID value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    public static ExpenseRequestId newId() {
        return new ExpenseRequestId(UUID.randomUUID());
    }

    public static ExpenseRequestId of(UUID value) {
        return new ExpenseRequestId(value);
    }

    public UUID value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof ExpenseRequestId that && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
