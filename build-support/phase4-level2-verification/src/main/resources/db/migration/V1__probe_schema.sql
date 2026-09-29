-- Tooling-only schema following Spring Modulith 2.1 PostgreSQL publication columns.
CREATE TABLE event_publication (
    id UUID PRIMARY KEY,
    listener_id TEXT NOT NULL,
    event_type TEXT NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date TIMESTAMP WITH TIME ZONE,
    status TEXT,
    completion_attempts INT,
    last_resubmission_date TIMESTAMP WITH TIME ZONE
);
CREATE INDEX event_publication_serialized_event_hash_idx
    ON event_publication USING hash (serialized_event);
CREATE INDEX event_publication_by_completion_date_idx
    ON event_publication (completion_date);

CREATE TABLE probe_approval (event_id UUID PRIMARY KEY);
CREATE TABLE probe_provider_send (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id UUID NOT NULL,
    idempotency_key UUID UNIQUE
);
