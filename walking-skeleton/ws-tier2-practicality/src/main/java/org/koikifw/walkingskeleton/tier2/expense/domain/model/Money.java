package org.koikifw.walkingskeleton.tier2.expense.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public final class Money {

    @Column(name = "amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal value;

    Money() {
        // JPA only
    }

    private Money(BigDecimal value) {
        Objects.requireNonNull(value, "value must not be null");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        this.value = value;
    }

    public static Money of(BigDecimal value) {
        return new Money(value);
    }

    public BigDecimal value() {
        return value;
    }

    public boolean hasSameValueAs(BigDecimal other) {
        return value.compareTo(other) == 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Money that && value.compareTo(that.value) == 0;
    }

    @Override
    public int hashCode() {
        return value.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
