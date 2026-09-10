package org.koikifw.audit.internal;

import org.koikifw.audit.AuditEvent;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

class BusinessAuditTransaction {

    private final AuditStore store;

    BusinessAuditTransaction(AuditStore store) {
        this.store = store;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditEvent event) {
        store.persistAndFlush("BUSINESS", event);
    }
}
