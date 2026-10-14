-- B Tooling only; formal Audit/Identity SQL is loaded from the actual JAR first.
CREATE ROLE s1b_owner NOLOGIN NOINHERIT;
CREATE ROLE s1b_web LOGIN NOINHERIT PASSWORD 's1-fixture-only';
CREATE ROLE s1b_worker LOGIN NOINHERIT PASSWORD 's1-fixture-only';
CREATE ROLE s1b_reader LOGIN NOINHERIT PASSWORD 's1-fixture-only';
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
CREATE SCHEMA s1b AUTHORIZATION s1b_owner;
SET ROLE s1b_owner;
CREATE TABLE s1b.permit (
 permit_id uuid PRIMARY KEY,
 environment_id text NOT NULL, publication_id uuid NOT NULL,
 event_id uuid NOT NULL, listener_id text NOT NULL, expected_attempt integer NOT NULL,
 actor_id text NOT NULL, reason_code text NOT NULL,
 issued_at timestamptz NOT NULL, expires_at timestamptz NOT NULL CHECK(expires_at > issued_at),
 closed_at timestamptz, confirmed_by text, result_ref text,
 version bigint NOT NULL DEFAULT 0,
 CHECK ((closed_at IS NULL AND confirmed_by IS NULL AND result_ref IS NULL)
 OR (closed_at IS NOT NULL AND confirmed_by IS NOT NULL AND result_ref IS NOT NULL))
);
CREATE UNIQUE INDEX s1b_unresolved_target ON s1b.permit(environment_id, publication_id)
 WHERE closed_at IS NULL;
CREATE TABLE s1b.consumption (
 permit_id uuid PRIMARY KEY REFERENCES s1b.permit(permit_id),
 operation_id uuid NOT NULL UNIQUE, worker_generation text NOT NULL, consumed_at timestamptz NOT NULL
);
RESET ROLE;
GRANT USAGE ON SCHEMA s1b TO s1b_web,s1b_worker,s1b_reader;
GRANT SELECT ON ALL TABLES IN SCHEMA s1b TO s1b_web,s1b_worker,s1b_reader;
GRANT INSERT (permit_id,environment_id,publication_id,event_id,listener_id,expected_attempt,
 actor_id,reason_code,issued_at,expires_at) ON s1b.permit TO s1b_web;
GRANT UPDATE (closed_at,confirmed_by,result_ref,version) ON s1b.permit TO s1b_web;
GRANT UPDATE (version) ON s1b.permit TO s1b_worker;
GRANT INSERT ON s1b.consumption TO s1b_worker;
GRANT SELECT ON koiki_user,koiki_role,koiki_permission,koiki_user_role,koiki_role_permission
 TO s1b_web,s1b_worker,s1b_reader;
GRANT INSERT ON koiki_audit_event TO s1b_web,s1b_worker;
GRANT SELECT ON koiki_audit_event TO s1b_reader;
