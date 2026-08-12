DO $$
BEGIN
    IF to_regclass('public.koiki_framework_marker') IS NULL THEN
        RAISE EXCEPTION 'KOIKI migration must run before Customer migration';
    END IF;
END $$;

CREATE TABLE kkbiz_expense (
    expense_id INTEGER PRIMARY KEY,
    amount INTEGER NOT NULL
);
