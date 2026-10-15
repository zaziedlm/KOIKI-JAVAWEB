-- Test-only schemas. Never registered with Flyway or packaged in the ordinary Tooling JAR.
CREATE SCHEMA b1_source;
CREATE SCHEMA b1_evidence;
CREATE SCHEMA b1_read;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE TABLE b1_source.binding (
 run uuid PRIMARY KEY, environment text NOT NULL, event uuid NOT NULL, publication uuid UNIQUE,
 notification_key text NOT NULL, payload_identity text NOT NULL, recipient_identity text NOT NULL,
 source text NOT NULL, key_until timestamptz NOT NULL
);
CREATE TABLE b1_source.snapshot (
 revision bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 environment text NOT NULL, publication uuid NOT NULL, event uuid NOT NULL,
 event_type text NOT NULL, listener text NOT NULL, attempt int NOT NULL,
 run uuid NOT NULL REFERENCES b1_source.binding(run), source text NOT NULL,
 fingerprint text NOT NULL, notification_key text NOT NULL, payload_identity text NOT NULL,
 recipient_identity text NOT NULL, status text NOT NULL, provider text NOT NULL, receipt bigint,
 generation bigint NOT NULL, all_ended boolean NOT NULL, forced boolean NOT NULL,
 controlled boolean NOT NULL, stop_observed timestamptz NOT NULL, stop_until timestamptz NOT NULL,
 observed timestamptz NOT NULL, valid_until timestamptz NOT NULL, key_until timestamptz NOT NULL
);
CREATE TABLE b1_evidence.document (
 reference uuid PRIMARY KEY, environment text NOT NULL, permit uuid NOT NULL, operation uuid NOT NULL,
 worker_generation bigint NOT NULL, run uuid NOT NULL, subject text NOT NULL,
 canonical text NOT NULL, valid_until timestamptz NOT NULL,
 UNIQUE(environment, permit, operation, worker_generation)
);
CREATE TABLE b1_evidence.revocation (
 reference uuid NOT NULL REFERENCES b1_evidence.document(reference), revoked_at timestamptz NOT NULL,
 PRIMARY KEY(reference, revoked_at)
);
CREATE FUNCTION b1_evidence.immutable() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'B1 immutable record'; END $$;
CREATE TRIGGER immutable_document BEFORE UPDATE OR DELETE ON b1_evidence.document
 FOR EACH ROW EXECUTE FUNCTION b1_evidence.immutable();
CREATE TRIGGER immutable_revocation BEFORE UPDATE OR DELETE ON b1_evidence.revocation
 FOR EACH ROW EXECUTE FUNCTION b1_evidence.immutable();
CREATE TRIGGER immutable_snapshot BEFORE UPDATE OR DELETE ON b1_source.snapshot
 FOR EACH ROW EXECUTE FUNCTION b1_evidence.immutable();
CREATE VIEW b1_read.target WITH (security_barrier=true) AS
 SELECT s.*, p.event_type AS current_event_type, p.listener_id AS current_listener,
 p.serialized_event AS current_serialized_event, p.status AS current_status,
 p.completion_attempts AS current_attempt, p.publication_date AS current_publication_date,
 p.completion_date AS current_completion_date, p.last_resubmission_date AS current_resubmission_date,
 b.publication AS bound_publication, b.notification_key AS bound_key,
 b.payload_identity AS bound_payload, b.recipient_identity AS bound_recipient,
 b.source AS bound_source, b.key_until AS bound_key_until,
 (SELECT max(t.revision) FROM b1_source.snapshot t WHERE t.run=s.run) AS current_revision,
 (SELECT count(*) FROM public.probe_provider_send r WHERE r.event_id=s.event OR r.idempotency_key=s.event) AS current_receipt_count,
 (SELECT min(r.id) FROM public.probe_provider_send r WHERE r.event_id=s.event AND r.idempotency_key=s.event) AS current_receipt
 FROM b1_source.snapshot s LEFT JOIN public.event_publication p ON p.id=s.publication
 JOIN b1_source.binding b ON b.run=s.run
 WHERE s.environment='b1-local' AND b.environment='b1-local';
CREATE VIEW b1_read.evidence WITH (security_barrier=true) AS
 SELECT * FROM b1_evidence.document WHERE environment='b1-local';
CREATE VIEW b1_read.revocation WITH (security_barrier=true) AS
 SELECT r.* FROM b1_evidence.revocation r JOIN b1_evidence.document d USING(reference)
 WHERE d.environment='b1-local';
GRANT USAGE ON SCHEMA b1_source,b1_evidence TO b1_writer;
GRANT SELECT,INSERT ON b1_source.snapshot,b1_evidence.document,b1_evidence.revocation TO b1_writer;
GRANT SELECT ON b1_source.binding,public.event_publication,public.probe_provider_send TO b1_writer;
GRANT USAGE ON SCHEMA public TO b1_writer;
GRANT USAGE,SELECT ON ALL SEQUENCES IN SCHEMA b1_source TO b1_writer;
GRANT USAGE ON SCHEMA b1_read TO b1_reader;
GRANT SELECT ON ALL TABLES IN SCHEMA b1_read TO b1_reader;
REVOKE ALL ON SCHEMA b1_source,b1_evidence FROM PUBLIC;
