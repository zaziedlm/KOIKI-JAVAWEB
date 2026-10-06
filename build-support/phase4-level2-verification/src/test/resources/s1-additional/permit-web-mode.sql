-- C-only setup, after actual Framework/Modulith artifact schemas. Not a migration.
CREATE ROLE s1c_owner NOLOGIN NOINHERIT;
CREATE ROLE s1c_web LOGIN NOINHERIT PASSWORD 's1-fixture-only';
CREATE ROLE s1c_check LOGIN NOINHERIT PASSWORD 's1-fixture-only';
CREATE ROLE s1c_recovery LOGIN NOINHERIT PASSWORD 's1-fixture-only';
CREATE ROLE s1c_observer LOGIN NOINHERIT PASSWORD 's1-fixture-only';
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA s1c AUTHORIZATION s1c_owner;
SET ROLE s1c_owner;
CREATE TABLE s1c.permit (
 permit_id uuid PRIMARY KEY, environment_id text NOT NULL, publication_id uuid NOT NULL,
 event_id uuid NOT NULL, listener_id text NOT NULL, expected_attempt integer NOT NULL,
 actor_id text NOT NULL, reason_code text NOT NULL,
 issued_at timestamptz NOT NULL, expires_at timestamptz NOT NULL CHECK(expires_at>issued_at),
 closed_at timestamptz, confirmed_by text, result_ref text, version bigint NOT NULL DEFAULT 0,
 CHECK ((closed_at IS NULL AND confirmed_by IS NULL AND result_ref IS NULL)
 OR (closed_at IS NOT NULL AND confirmed_by IS NOT NULL AND result_ref IS NOT NULL))
);
CREATE UNIQUE INDEX s1c_unresolved_target ON s1c.permit(environment_id,publication_id) WHERE closed_at IS NULL;
CREATE TABLE s1c.consumption (
 permit_id uuid PRIMARY KEY REFERENCES s1c.permit(permit_id), operation_id uuid NOT NULL UNIQUE,
 worker_generation text NOT NULL, consumed_at timestamptz NOT NULL
);
RESET ROLE;
GRANT USAGE ON SCHEMA s1c TO s1c_web,s1c_check,s1c_recovery,s1c_observer;
GRANT SELECT ON ALL TABLES IN SCHEMA s1c TO s1c_web,s1c_check,s1c_recovery,s1c_observer;
GRANT INSERT (permit_id,environment_id,publication_id,event_id,listener_id,expected_attempt,actor_id,
 reason_code,issued_at,expires_at) ON s1c.permit TO s1c_web;
GRANT UPDATE (closed_at,confirmed_by,result_ref,version) ON s1c.permit TO s1c_web;
GRANT UPDATE (version) ON s1c.permit TO s1c_recovery;
GRANT INSERT ON s1c.consumption TO s1c_recovery;
GRANT SELECT ON event_publication TO s1c_web,s1c_check,s1c_recovery,s1c_observer;
GRANT UPDATE (completion_date,status,completion_attempts,last_resubmission_date)
 ON event_publication TO s1c_recovery;
GRANT SELECT ON koiki_user,koiki_role,koiki_permission,koiki_user_role,koiki_role_permission
 TO s1c_web,s1c_check,s1c_recovery,s1c_observer;
GRANT INSERT ON koiki_audit_event TO s1c_web,s1c_check,s1c_recovery;
GRANT SELECT ON koiki_audit_event,koiki_session,koiki_session_attributes,koiki_login_attempt TO s1c_observer;
GRANT SELECT ON koiki_password_credential TO s1c_web,s1c_check;
GRANT UPDATE (locked_until,version,updated_at) ON koiki_password_credential TO s1c_web,s1c_check;
GRANT SELECT,INSERT,UPDATE,DELETE ON koiki_login_attempt,koiki_session,koiki_session_attributes TO s1c_web,s1c_check;
