package org.koikifw.reference.expense.domain.model;

import jakarta.persistence.Embeddable;

/** Positive whole-yen amount used by the Reference application. */
@Embeddable
public record Money(long amount) {

    public Money {
        if (amount <= 0) {
            throw new ExpenseDomainException(ExpenseRuleViolation.INVALID_CONTENT);
        }
    }

    static Money sum(Iterable<ExpenseLine> lines) {
        long total = 0;
        try {
            for (ExpenseLine line : lines) {
                total = Math.addExact(total, line.amount().amount());
            }
        } catch (ArithmeticException exception) {
            throw new ExpenseDomainException(ExpenseRuleViolation.INVALID_CONTENT);
        }
        return new Money(total);
    }
}

