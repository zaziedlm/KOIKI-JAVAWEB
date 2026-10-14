CREATE TABLE kkref_notification_async.notification_receipt_observation (
  notification_key TEXT PRIMARY KEY,
  event_id UUID NOT NULL,
  recipient_id UUID NOT NULL,
  payload_sha256 TEXT NOT NULL,
  acceptance_id UUID NOT NULL,
  observed_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT ck_kkref_notification_receipt_key CHECK
    (btrim(notification_key) <> '' AND char_length(notification_key) <= 200),
  CONSTRAINT ck_kkref_notification_receipt_hash CHECK
    (payload_sha256 ~ '^[0-9a-f]{64}$')
);
CREATE UNIQUE INDEX uk_kkref_notification_receipt_acceptance
  ON kkref_notification_async.notification_receipt_observation (acceptance_id);
