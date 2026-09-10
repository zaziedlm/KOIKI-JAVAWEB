CREATE TABLE koiki_audit_event (
    event_id UUID PRIMARY KEY,
    audit_type VARCHAR(16) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    actor_type VARCHAR(16) NOT NULL,
    actor_id VARCHAR(255),
    subject_id VARCHAR(255),
    resource_type VARCHAR(128),
    resource_id VARCHAR(255),
    action VARCHAR(128) NOT NULL,
    result VARCHAR(16) NOT NULL,
    reason_code VARCHAR(128),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    request_id VARCHAR(128),
    trace_id VARCHAR(128),
    CONSTRAINT fixture_audit_failure CHECK (event_type <> 'FORCE_AUDIT_FAILURE')
);

CREATE TABLE fixture_business_change (
    change_id UUID PRIMARY KEY,
    change_value VARCHAR(128) NOT NULL
);
