package org.koikifw.audit.internal;

import org.koikifw.audit.AuditEvent;
import org.koikifw.audit.AuditRecordingException;
import org.koikifw.audit.SecurityAuditRecorder;

final class DefaultSecurityAuditRecorder implements SecurityAuditRecorder {

    private final SecurityAuditTransaction transaction;

    DefaultSecurityAuditRecorder(SecurityAuditTransaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public void record(AuditEvent event) {
        try {
            transaction.record(event);
        } catch (AuditRecordingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AuditRecordingException();
        }
    }
}
