package com.example.customer.sales.application;

import com.example.customer.inventory.internal.InternalInventory;

public class SalesUseCase {

    private final InternalInventory inventory = new InternalInventory();

    public boolean canSell() {
        return inventory != null;
    }
}
