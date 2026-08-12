CREATE TABLE koiki_framework_marker (
    marker_id INTEGER PRIMARY KEY,
    marker_name VARCHAR(100) NOT NULL
);

INSERT INTO koiki_framework_marker (marker_id, marker_name)
VALUES (1, 'KOIKI migration ran before Customer migration');
