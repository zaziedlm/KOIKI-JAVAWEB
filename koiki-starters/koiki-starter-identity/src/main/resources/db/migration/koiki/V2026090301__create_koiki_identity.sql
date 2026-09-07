CREATE TABLE koiki_user (
    user_id uuid PRIMARY KEY,
    email varchar(320) NOT NULL,
    canonical_email varchar(320) NOT NULL,
    status varchar(16) NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamp(6) with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp(6) with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_koiki_user_canonical_email UNIQUE (canonical_email),
    CONSTRAINT ck_koiki_user_email_trimmed CHECK (email = btrim(email)),
    CONSTRAINT ck_koiki_user_email_canonical_consistency
        CHECK (canonical_email = lower(email)),
    CONSTRAINT ck_koiki_user_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT ck_koiki_user_version CHECK (version >= 0)
);

CREATE TABLE koiki_role (
    role_id uuid PRIMARY KEY,
    role_code varchar(100) NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamp(6) with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp(6) with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_koiki_role_code UNIQUE (role_code),
    CONSTRAINT ck_koiki_role_code CHECK (role_code ~ '^[A-Z][A-Z0-9_:]{0,99}$'),
    CONSTRAINT ck_koiki_role_version CHECK (version >= 0)
);

CREATE TABLE koiki_permission (
    permission_id uuid PRIMARY KEY,
    permission_code varchar(100) NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamp(6) with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp(6) with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_koiki_permission_code UNIQUE (permission_code),
    CONSTRAINT ck_koiki_permission_code
        CHECK (permission_code ~ '^[A-Z][A-Z0-9_:]{0,99}$'),
    CONSTRAINT ck_koiki_permission_version CHECK (version >= 0)
);

CREATE TABLE koiki_user_role (
    user_id uuid NOT NULL,
    role_id uuid NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_koiki_user_role_user FOREIGN KEY (user_id)
        REFERENCES koiki_user (user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_koiki_user_role_role FOREIGN KEY (role_id)
        REFERENCES koiki_role (role_id) ON DELETE RESTRICT
);

CREATE TABLE koiki_role_permission (
    role_id uuid NOT NULL,
    permission_id uuid NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_koiki_role_permission_role FOREIGN KEY (role_id)
        REFERENCES koiki_role (role_id) ON DELETE RESTRICT,
    CONSTRAINT fk_koiki_role_permission_permission FOREIGN KEY (permission_id)
        REFERENCES koiki_permission (permission_id) ON DELETE RESTRICT
);

CREATE TABLE koiki_password_credential (
    user_id uuid PRIMARY KEY,
    encoded_password varchar(512) NOT NULL,
    locked_until timestamp(6) with time zone,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamp(6) with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp(6) with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_koiki_password_credential_user FOREIGN KEY (user_id)
        REFERENCES koiki_user (user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_koiki_password_credential_value CHECK (btrim(encoded_password) <> ''),
    CONSTRAINT ck_koiki_password_credential_version CHECK (version >= 0)
);

CREATE TABLE koiki_login_attempt (
    attempt_id uuid PRIMARY KEY,
    scope varchar(16) NOT NULL,
    user_id uuid,
    source_key_id varchar(100),
    source_fingerprint bytea,
    failure_count integer NOT NULL DEFAULT 0,
    window_started_at timestamp(6) with time zone NOT NULL,
    last_failed_at timestamp(6) with time zone NOT NULL,
    blocked_until timestamp(6) with time zone,
    created_at timestamp(6) with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp(6) with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_koiki_login_attempt_user FOREIGN KEY (user_id)
        REFERENCES koiki_user (user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_koiki_login_attempt_count CHECK (failure_count >= 0),
    CONSTRAINT ck_koiki_login_attempt_scope CHECK (
        (scope = 'ACCOUNT'
            AND user_id IS NOT NULL
            AND source_key_id IS NULL
            AND source_fingerprint IS NULL)
        OR
        (scope = 'SOURCE'
            AND user_id IS NULL
            AND source_key_id IS NOT NULL
            AND btrim(source_key_id) <> ''
            AND source_fingerprint IS NOT NULL
            AND octet_length(source_fingerprint) = 32)
    )
);

CREATE UNIQUE INDEX uk_koiki_login_attempt_account
    ON koiki_login_attempt (user_id)
    WHERE scope = 'ACCOUNT';

CREATE UNIQUE INDEX uk_koiki_login_attempt_source
    ON koiki_login_attempt (source_key_id, source_fingerprint)
    WHERE scope = 'SOURCE';

CREATE TABLE koiki_external_identity_link (
    link_id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    issuer varchar(2048) NOT NULL,
    subject varchar(255) NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamp(6) with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp(6) with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_koiki_external_identity_link_user FOREIGN KEY (user_id)
        REFERENCES koiki_user (user_id) ON DELETE RESTRICT,
    CONSTRAINT uk_koiki_external_identity_subject UNIQUE (issuer, subject),
    CONSTRAINT uk_koiki_external_identity_user_issuer UNIQUE (user_id, issuer),
    CONSTRAINT ck_koiki_external_identity_issuer CHECK (btrim(issuer) <> ''),
    CONSTRAINT ck_koiki_external_identity_subject CHECK (btrim(subject) <> ''),
    CONSTRAINT ck_koiki_external_identity_version CHECK (version >= 0)
);
