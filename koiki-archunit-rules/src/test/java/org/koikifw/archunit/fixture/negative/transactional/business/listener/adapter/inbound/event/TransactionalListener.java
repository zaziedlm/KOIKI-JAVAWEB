package org.koikifw.archunit.fixture.negative.transactional.business.listener.adapter.inbound.event;

import org.springframework.transaction.event.TransactionalEventListener;

public final class TransactionalListener {

    @TransactionalEventListener
    public void on(String event) {
    }
}
