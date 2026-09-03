package org.koikifw.audit;

/** Records an audit event in an independent transaction. */
public interface SecurityAuditRecorder {

    /**
     * Records and commits the event independently of any caller transaction.
     *
     * @param event safe audit data
     * @throws AuditRecordingException when persistence fails
     */
    void record(AuditEvent event);
}
