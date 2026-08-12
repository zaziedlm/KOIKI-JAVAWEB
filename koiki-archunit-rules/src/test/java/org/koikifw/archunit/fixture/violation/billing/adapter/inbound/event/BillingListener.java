package org.koikifw.archunit.fixture.violation.billing.adapter.inbound.event;

import org.springframework.transaction.event.TransactionalEventListener;

public class BillingListener {

    @TransactionalEventListener
    public void onBillingCompleted() {
    }
}
