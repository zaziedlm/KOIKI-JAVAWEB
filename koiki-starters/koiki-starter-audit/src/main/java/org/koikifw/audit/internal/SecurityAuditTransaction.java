package org.koikifw.audit.internal;

import org.koikifw.audit.AuditEvent;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

class SecurityAuditTransaction {

    private final AuditStore store;

    SecurityAuditTransaction(AuditStore store) {
        this.store = store;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditEvent event) {
        store.persistAndFlush("SECURITY", event);
    }
}
