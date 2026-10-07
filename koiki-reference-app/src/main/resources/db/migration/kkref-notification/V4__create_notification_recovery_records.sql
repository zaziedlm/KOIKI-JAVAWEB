CREATE TABLE kkref_notification_recovery_permit (
    permit_id UUID PRIMARY KEY,
    environment_id VARCHAR(128) NOT NULL,
    publication_id UUID NOT NULL,
    event_id UUID NOT NULL,
    listener_id TEXT NOT NULL,
    expected_attempt INTEGER NOT NULL,
    actor_id UUID NOT NULL,
    reason_code VARCHAR(128) NOT NULL,
    issued_at TIMESTAMPTZ(6) NOT NULL,
    expires_at TIMESTAMPTZ(6) NOT NULL,
    closed_at TIMESTAMPTZ(6),
    confirmed_by UUID,
    result_ref VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_kkref_notification_permit_environment CHECK (btrim(environment_id) <> ''),
    CONSTRAINT ck_kkref_notification_permit_listener CHECK (btrim(listener_id) <> ''),
    CONSTRAINT ck_kkref_notification_permit_reason CHECK (btrim(reason_code) <> ''),
    CONSTRAINT ck_kkref_notification_permit_attempt CHECK (expected_attempt >= 0),
    CONSTRAINT ck_kkref_notification_permit_validity CHECK (expires_at > issued_at),
    CONSTRAINT ck_kkref_notification_permit_version CHECK (version >= 0),
    CONSTRAINT ck_kkref_notification_permit_closure CHECK (
        (closed_at IS NULL AND confirmed_by IS NULL AND result_ref IS NULL)
        OR (closed_at IS NOT NULL AND confirmed_by IS NOT NULL
            AND result_ref IS NOT NULL AND btrim(result_ref) <> '')
    )
);

CREATE UNIQUE INDEX uk_kkref_notification_permit_unclosed_target
    ON kkref_notification_recovery_permit (environment_id, publication_id)
    WHERE closed_at IS NULL;

CREATE TABLE kkref_notification_recovery_consumption (
    permit_id UUID PRIMARY KEY,
    operation_id UUID NOT NULL UNIQUE,
    worker_generation VARCHAR(128) NOT NULL,
    consumed_at TIMESTAMPTZ(6) NOT NULL,
    CONSTRAINT fk_kkref_notification_consumption_permit FOREIGN KEY (permit_id)
        REFERENCES kkref_notification_recovery_permit (permit_id),
    CONSTRAINT ck_kkref_notification_consumption_worker CHECK (btrim(worker_generation) <> '')
);
