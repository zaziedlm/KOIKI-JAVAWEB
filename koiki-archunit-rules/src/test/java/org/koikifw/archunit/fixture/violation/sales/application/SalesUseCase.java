package org.koikifw.archunit.fixture.violation.sales.application;

import org.koikifw.archunit.fixture.violation.inventory.application.InventoryUseCase;
import org.koikifw.archunit.fixture.violation.inventory.internal.InternalStock;

public class SalesUseCase {

    private final InventoryUseCase inventoryUseCase = new InventoryUseCase();
    private final InternalStock internalStock = new InternalStock();

    public boolean canSell() {
        return inventoryUseCase != null && internalStock != null;
    }
}
