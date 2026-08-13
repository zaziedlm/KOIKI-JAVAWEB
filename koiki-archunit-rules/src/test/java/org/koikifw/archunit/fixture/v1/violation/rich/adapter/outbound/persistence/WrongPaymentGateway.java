package org.koikifw.archunit.fixture.v1.violation.rich.adapter.outbound.persistence;

import org.koikifw.archunit.fixture.v1.violation.rich.domain.gateway.PaymentGateway;

public class WrongPaymentGateway implements PaymentGateway {
    @Override
    public void pay() {
    }
}
