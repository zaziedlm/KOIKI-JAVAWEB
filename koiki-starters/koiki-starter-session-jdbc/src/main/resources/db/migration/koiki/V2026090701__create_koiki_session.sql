CREATE TABLE koiki_session (
    primary_id char(36) NOT NULL,
    session_id char(36) NOT NULL,
    creation_time bigint NOT NULL,
    last_access_time bigint NOT NULL,
    max_inactive_interval integer NOT NULL,
    expiry_time bigint NOT NULL,
    principal_name varchar(100),
    CONSTRAINT pk_koiki_session PRIMARY KEY (primary_id)
);

CREATE UNIQUE INDEX uk_koiki_session_session_id
    ON koiki_session (session_id);

CREATE INDEX ix_koiki_session_expiry_time
    ON koiki_session (expiry_time);

CREATE INDEX ix_koiki_session_principal_name
    ON koiki_session (principal_name);

CREATE TABLE koiki_session_attributes (
    session_primary_id char(36) NOT NULL,
    attribute_name varchar(200) NOT NULL,
    attribute_bytes bytea NOT NULL,
    CONSTRAINT pk_koiki_session_attributes
        PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT fk_koiki_session_attributes_session
        FOREIGN KEY (session_primary_id)
        REFERENCES koiki_session (primary_id) ON DELETE CASCADE
);
