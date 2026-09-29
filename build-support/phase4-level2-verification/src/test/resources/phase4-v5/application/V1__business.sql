-- Tooling-only business tables remain Application-owned.
CREATE TABLE probe_approval (event_id UUID PRIMARY KEY);
CREATE TABLE probe_provider_send (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id UUID NOT NULL,
    idempotency_key UUID UNIQUE
);
