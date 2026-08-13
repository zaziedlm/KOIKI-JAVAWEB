package org.koikifw.archunit.fixture.compliant.sales.application.query;

import org.koikifw.archunit.fixture.compliant.sales.readmodel.SalesSummary;

public class SalesQuery {
    public SalesSummary summary() {
        return new SalesSummary(0);
    }
}
