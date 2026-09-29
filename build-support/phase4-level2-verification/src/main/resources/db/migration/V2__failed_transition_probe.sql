-- Tooling-only PostgreSQL marker. This is not a Modulith or Framework migration proposal.
ALTER TABLE event_publication
    ADD COLUMN probe_failed_at TIMESTAMP WITH TIME ZONE;

CREATE FUNCTION probe_mark_failed_transition() RETURNS trigger AS $$
BEGIN
    IF NEW.status = 'FAILED' AND OLD.status IS DISTINCT FROM 'FAILED' THEN
        NEW.probe_failed_at := clock_timestamp();
    ELSIF NEW.status IS DISTINCT FROM 'FAILED' THEN
        NEW.probe_failed_at := NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER probe_failed_transition
    BEFORE UPDATE OF status ON event_publication
    FOR EACH ROW EXECUTE FUNCTION probe_mark_failed_transition();
