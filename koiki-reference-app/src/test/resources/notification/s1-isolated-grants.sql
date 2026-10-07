-- Isolated verification only. Credentials and role creation are supplied in memory by the harness.
-- Never applied by Flyway or bundled as a production migration.
ALTER TABLE kkref_notification_recovery_permit OWNER TO kkref_notification_owner;
ALTER TABLE kkref_notification_recovery_consumption OWNER TO kkref_notification_owner;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO kkref_notification_permit, kkref_notification_consumer, kkref_notification_reader;
GRANT SELECT ON kkref_notification_recovery_permit, kkref_notification_recovery_consumption
    TO kkref_notification_permit, kkref_notification_consumer, kkref_notification_reader;
GRANT INSERT (permit_id, environment_id, publication_id, event_id, listener_id, expected_attempt,
    actor_id, reason_code, issued_at, expires_at) ON kkref_notification_recovery_permit TO kkref_notification_permit;
GRANT UPDATE (closed_at, confirmed_by, result_ref, version) ON kkref_notification_recovery_permit TO kkref_notification_permit;
GRANT UPDATE (version) ON kkref_notification_recovery_permit TO kkref_notification_consumer;
GRANT INSERT (permit_id, operation_id, worker_generation, consumed_at)
    ON kkref_notification_recovery_consumption TO kkref_notification_consumer;
-- Public IdentityQuery's current five read tables only; no password/login/session access.
GRANT SELECT ON koiki_user, koiki_role, koiki_permission, koiki_user_role, koiki_role_permission
    TO kkref_notification_permit, kkref_notification_consumer, kkref_notification_reader;
-- Public Recorders use UUID IDs; no sequence or Audit SELECT grants are needed.
GRANT INSERT ON koiki_audit_event TO kkref_notification_permit, kkref_notification_consumer, kkref_notification_reader;
