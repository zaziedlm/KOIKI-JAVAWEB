ALTER TABLE koiki_audit_event
    ADD CONSTRAINT fixture_audit_failure
    CHECK (event_type <> 'FORCE_AUDIT_FAILURE');
