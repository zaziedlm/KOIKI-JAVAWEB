package org.koikifw.reference.expense.adapter.inbound.api;

/** Adapter-local signal for an absent or out-of-scope expense request. */
final class ExpenseApiNotFoundException extends RuntimeException {

    ExpenseApiNotFoundException() {
        super("expense request is not available");
    }
}
