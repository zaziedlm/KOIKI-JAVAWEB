-- Non-distributed B2 fixture protocol. No Flyway registration.
CREATE SCHEMA b2_source;
CREATE SCHEMA b2_read;
REVOKE ALL ON SCHEMA b2_source,b2_read FROM PUBLIC;
CREATE TABLE b2_source.freeze (
 run uuid PRIMARY KEY,
 protocol_version integer NOT NULL CHECK(protocol_version=1),
 environment text NOT NULL, publication uuid NOT NULL, event uuid NOT NULL,
 event_type text NOT NULL, listener text NOT NULL, attempt integer NOT NULL,
 source_id text NOT NULL, jar_sha256 text NOT NULL, revision bigint NOT NULL,
 fingerprint text NOT NULL, notification_key text NOT NULL,
 payload_identity text NOT NULL, recipient_identity text NOT NULL,
 generation bigint NOT NULL, frozen_at timestamptz NOT NULL, admit_until timestamptz NOT NULL,
 publication_date timestamptz NOT NULL, completion_date timestamptz,
 last_resubmission_date timestamptz, serialized_event text NOT NULL, status text NOT NULL
);
CREATE TRIGGER immutable_freeze BEFORE UPDATE OR DELETE ON b2_source.freeze
 FOR EACH ROW EXECUTE FUNCTION b1_evidence.immutable();
CREATE VIEW b2_read.frozen_target WITH (security_barrier=true) AS
 SELECT f.protocol_version,f.run,f.environment,f.publication,f.event,f.event_type,f.listener,f.attempt,
 f.source_id,f.jar_sha256,f.revision,f.fingerprint,f.notification_key,f.payload_identity,
 f.recipient_identity,f.generation,f.frozen_at,f.admit_until,
 t.observed,t.valid_until,t.key_until,t.provider,t.all_ended,t.controlled,
 (t.current_revision=f.revision AND t.source=f.source_id AND t.fingerprint=f.fingerprint
  AND t.event=f.event AND t.event_type=f.event_type AND t.listener=f.listener AND t.attempt=f.attempt
  AND t.bound_publication=f.publication AND t.bound_key=f.notification_key
  AND t.bound_payload=f.payload_identity AND t.bound_recipient=f.recipient_identity
  AND t.notification_key=f.notification_key AND t.payload_identity=f.payload_identity
  AND t.recipient_identity=f.recipient_identity AND t.bound_source=f.source_id
  AND t.bound_key_until=t.key_until AND t.generation=f.generation
  AND t.current_event_type=f.event_type AND t.current_listener=f.listener
  AND t.current_attempt=f.attempt AND t.current_serialized_event=f.serialized_event
  AND t.current_status=f.status AND t.current_publication_date=f.publication_date
  AND t.current_completion_date IS NOT DISTINCT FROM f.completion_date
  AND t.current_resubmission_date IS NOT DISTINCT FROM f.last_resubmission_date
  AND t.current_receipt_count=0 AND t.current_receipt IS NULL) AS current_matches,
 (SELECT count(*)=2 FROM pg_roles r WHERE r.rolname IN ('b1_fixture','b1_writer')
  AND NOT r.rolcanlogin AND NOT r.rolsuper AND NOT r.rolcreatedb AND NOT r.rolcreaterole
  AND NOT EXISTS(SELECT 1 FROM pg_auth_members m WHERE m.member=r.oid)) AS writers_disabled
 FROM b2_source.freeze f JOIN b1_read.target t
 ON t.run=f.run AND t.revision=f.revision AND t.environment=f.environment AND t.publication=f.publication;
GRANT USAGE ON SCHEMA b2_read TO b2_reader;
GRANT SELECT ON b2_read.frozen_target TO b2_reader;
