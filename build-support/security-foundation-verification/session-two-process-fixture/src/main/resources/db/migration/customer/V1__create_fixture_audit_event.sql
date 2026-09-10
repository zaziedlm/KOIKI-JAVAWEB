CREATE TABLE koiki_audit_event (
    event_id uuid PRIMARY KEY,
    audit_type varchar(16) NOT NULL,
    event_type varchar(128) NOT NULL,
    actor_type varchar(16) NOT NULL,
    actor_id varchar(255),
    subject_id varchar(255),
    resource_type varchar(128),
    resource_id varchar(255),
    action varchar(128) NOT NULL,
    result varchar(16) NOT NULL,
    reason_code varchar(128),
    occurred_at timestamp with time zone NOT NULL,
    request_id varchar(128),
    trace_id varchar(128),
    CONSTRAINT fixture_audit_failure CHECK (event_type <> 'FORCE_AUDIT_FAILURE')
);
