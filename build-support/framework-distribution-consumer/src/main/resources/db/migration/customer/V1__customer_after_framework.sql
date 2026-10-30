-- These lookups fail if Framework migrations have not run in the same schema.
SELECT event_id FROM koiki_audit_event LIMIT 0;
SELECT user_id FROM koiki_user LIMIT 0;
CREATE TABLE customer_record (
    id bigint PRIMARY KEY,
    description varchar(255) NOT NULL
);
