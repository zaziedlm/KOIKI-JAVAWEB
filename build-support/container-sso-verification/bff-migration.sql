-- V1: Tooling-owned store. No business data, IdP sid or cross-DB foreign keys.
CREATE TABLE bff_auth_transaction (
    client_id text NOT NULL CHECK (client_id IN ('a', 'b')),
    state_hash char(64) NOT NULL CHECK (state_hash ~ '^[a-f0-9]{64}$'),
    browser_binding_hash char(64) NOT NULL CHECK (browser_binding_hash ~ '^[a-f0-9]{64}$'),
    protected_payload bytea NOT NULL,
    encryption_algorithm text NOT NULL CHECK (encryption_algorithm = 'aes-256-gcm'),
    encryption_key_id text NOT NULL,
    encryption_iv bytea NOT NULL CHECK (octet_length(encryption_iv) = 12),
    encryption_tag bytea NOT NULL CHECK (octet_length(encryption_tag) = 16),
    created_at timestamptz NOT NULL DEFAULT current_timestamp,
    expires_at timestamptz NOT NULL,
    consumed_at timestamptz,
    PRIMARY KEY (client_id, state_hash),
    CHECK (expires_at > created_at),
    CHECK (consumed_at IS NULL OR consumed_at >= created_at)
);
CREATE INDEX bff_auth_transaction_expiry ON bff_auth_transaction(expires_at);

CREATE TABLE bff_session (
    client_id text NOT NULL CHECK (client_id IN ('a', 'b')),
    session_id_hash char(64) NOT NULL CHECK (session_id_hash ~ '^[a-f0-9]{64}$'),
    issuer text NOT NULL,
    subject text NOT NULL,
    csrf_hash char(64) NOT NULL CHECK (csrf_hash ~ '^[a-f0-9]{64}$'),
    protected_tokens bytea NOT NULL,
    encryption_algorithm text NOT NULL CHECK (encryption_algorithm = 'aes-256-gcm'),
    encryption_key_id text NOT NULL,
    encryption_iv bytea NOT NULL CHECK (octet_length(encryption_iv) = 12),
    encryption_tag bytea NOT NULL CHECK (octet_length(encryption_tag) = 16),
    access_token_expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT current_timestamp,
    last_seen_at timestamptz NOT NULL DEFAULT current_timestamp,
    idle_expires_at timestamptz NOT NULL,
    absolute_expires_at timestamptz NOT NULL,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    PRIMARY KEY (client_id, session_id_hash),
    CHECK (last_seen_at >= created_at),
    CHECK (idle_expires_at > created_at AND idle_expires_at <= absolute_expires_at),
    CHECK (absolute_expires_at > created_at)
);
CREATE INDEX bff_session_idle_expiry ON bff_session(idle_expires_at);
CREATE INDEX bff_session_absolute_expiry ON bff_session(absolute_expires_at);
