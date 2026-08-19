package org.koikifw.archunit.fixture.compliant.sales.application.query;

public class SalesQuery {
    public SalesSummary summary() {
        return new SalesSummary(0);
    }
}
