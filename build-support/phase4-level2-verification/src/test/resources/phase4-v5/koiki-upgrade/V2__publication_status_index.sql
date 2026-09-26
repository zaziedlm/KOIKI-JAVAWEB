-- Probe an independent KOIKI upgrade after Application V1.
CREATE INDEX event_publication_status_date_idx
    ON event_publication (status, publication_date);
