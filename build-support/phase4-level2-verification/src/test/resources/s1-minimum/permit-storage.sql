-- Tooling-only DDL, explicitly loaded by L1; outside Flyway auto-discovery.
-- Credentials and identities below are disposable test data, never application authentication.
CREATE ROLE s1_owner NOLOGIN NOINHERIT;
CREATE ROLE s1_web LOGIN NOINHERIT PASSWORD 's1-fixture-only';
CREATE ROLE s1_worker LOGIN NOINHERIT PASSWORD 's1-fixture-only';
CREATE ROLE s1_reader LOGIN NOINHERIT PASSWORD 's1-fixture-only';
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA s1 AUTHORIZATION s1_owner;
SET ROLE s1_owner;
CREATE TABLE s1.permit (
    permit_id uuid PRIMARY KEY,
    environment_id text NOT NULL,
    publication_id uuid NOT NULL,
    event_id uuid NOT NULL,
    listener_id text NOT NULL,
    actor_id text NOT NULL,
    reason_code text NOT NULL,
    issued_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL CHECK (expires_at > issued_at),
    closed_at timestamptz,
    confirmed_by text,
    result_ref text,
    version bigint NOT NULL DEFAULT 0,
    CHECK ((closed_at IS NULL AND confirmed_by IS NULL AND result_ref IS NULL)
        OR (closed_at IS NOT NULL AND confirmed_by IS NOT NULL AND result_ref IS NOT NULL))
);
CREATE UNIQUE INDEX s1_unresolved_target ON s1.permit(environment_id, publication_id)
    WHERE closed_at IS NULL;
CREATE TABLE s1.consumption (
    permit_id uuid PRIMARY KEY REFERENCES s1.permit(permit_id),
    operation_id uuid NOT NULL UNIQUE,
    worker_generation text NOT NULL,
    consumed_at timestamptz NOT NULL
);
-- This probe tests transaction atomicity only, not Framework BusinessAuditRecorder.
CREATE TABLE s1.audit_probe (
    permit_id uuid NOT NULL REFERENCES s1.permit(permit_id),
    action text NOT NULL
);
RESET ROLE;
GRANT USAGE ON SCHEMA s1 TO s1_web, s1_worker, s1_reader;
GRANT SELECT ON ALL TABLES IN SCHEMA s1 TO s1_web, s1_worker, s1_reader;
GRANT INSERT (permit_id, environment_id, publication_id, event_id, listener_id,
    actor_id, reason_code, issued_at, expires_at) ON s1.permit TO s1_web;
GRANT UPDATE (closed_at, confirmed_by, result_ref, version) ON s1.permit TO s1_web;
GRANT UPDATE (version) ON s1.permit TO s1_worker;
GRANT INSERT ON s1.consumption TO s1_worker;
GRANT INSERT ON s1.audit_probe TO s1_web, s1_worker;
