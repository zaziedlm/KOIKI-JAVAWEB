package org.koikifw.audit;

/** Records an audit event in the caller's business transaction. */
public interface BusinessAuditRecorder {

    /**
     * Records the event in an existing transaction.
     *
     * @param event safe audit data
     * @throws AuditRecordingException when no transaction exists or persistence fails
     */
    void record(AuditEvent event);
}
