-- One-time repair of the original D3 fixture, not an application migration.
-- Backup the dedicated DB first. Any unexpected state rolls the entire transaction back.
\set ON_ERROR_STOP on
BEGIN;
LOCK TABLE kkref_department, kkref_expense_category, kkref_user_department_assignment,
    kkref_expense_approver_scope, kkref_expense_request, kkref_expense_line, koiki_audit_event
    IN SHARE ROW EXCLUSIVE MODE;
DO $$ BEGIN
    IF current_database() <> 'koiki'
       OR (SELECT count(*) FROM kkref_department WHERE department_id='d3000000-0000-0000-0000-000000000004'
           AND department_code='D3' AND department_name='D3 Department') <> 1
       OR (SELECT count(*) FROM kkref_expense_category WHERE expense_category_id='d3000000-0000-0000-0000-000000000005'
           AND expense_category_code='D3_TRAVEL' AND expense_category_name='D3 Travel') <> 1
       OR EXISTS(SELECT 1 FROM kkref_department WHERE department_id='d3000000-0000-4000-8000-000000000004')
       OR EXISTS(SELECT 1 FROM kkref_expense_category WHERE expense_category_id='d3000000-0000-4000-8000-000000000005')
    THEN RAISE EXCEPTION 'Not the original D3 fixture; stop without changes'; END IF;
END $$;
CREATE TEMP TABLE d3_before_request ON COMMIT DROP AS
    SELECT expense_request_id, to_jsonb(r)-'department_id' AS business FROM kkref_expense_request r;
CREATE TEMP TABLE d3_before_line ON COMMIT DROP AS
    SELECT expense_line_id, to_jsonb(l)-'expense_category_id' AS business FROM kkref_expense_line l;
CREATE TEMP TABLE d3_before_audit ON COMMIT DROP AS
    SELECT to_jsonb(a) AS audit FROM koiki_audit_event a;
INSERT INTO kkref_department
 SELECT 'd3000000-0000-4000-8000-000000000004'::uuid, 'D3_UUID_FIX', department_name, active, version, created_at, updated_at
 FROM kkref_department WHERE department_id='d3000000-0000-0000-0000-000000000004';
INSERT INTO kkref_expense_category
 SELECT 'd3000000-0000-4000-8000-000000000005'::uuid, 'D3_TRAVEL_UUID_FIX', expense_category_name, active, version, created_at, updated_at
 FROM kkref_expense_category WHERE expense_category_id='d3000000-0000-0000-0000-000000000005';
UPDATE kkref_user_department_assignment SET department_id='d3000000-0000-4000-8000-000000000004'
 WHERE department_id='d3000000-0000-0000-0000-000000000004';
UPDATE kkref_expense_approver_scope SET department_id='d3000000-0000-4000-8000-000000000004'
 WHERE department_id='d3000000-0000-0000-0000-000000000004';
UPDATE kkref_expense_request SET department_id='d3000000-0000-4000-8000-000000000004'
 WHERE department_id='d3000000-0000-0000-0000-000000000004';
UPDATE kkref_expense_line SET expense_category_id='d3000000-0000-4000-8000-000000000005'
 WHERE expense_category_id='d3000000-0000-0000-0000-000000000005';
DELETE FROM kkref_department WHERE department_id='d3000000-0000-0000-0000-000000000004';
DELETE FROM kkref_expense_category WHERE expense_category_id='d3000000-0000-0000-0000-000000000005';
UPDATE kkref_department SET department_code='D3' WHERE department_id='d3000000-0000-4000-8000-000000000004';
UPDATE kkref_expense_category SET expense_category_code='D3_TRAVEL' WHERE expense_category_id='d3000000-0000-4000-8000-000000000005';
DO $$ BEGIN
    IF EXISTS(SELECT * FROM d3_before_request EXCEPT SELECT expense_request_id,to_jsonb(r)-'department_id' FROM kkref_expense_request r)
       OR EXISTS(SELECT expense_request_id,to_jsonb(r)-'department_id' FROM kkref_expense_request r EXCEPT SELECT * FROM d3_before_request)
       OR EXISTS(SELECT * FROM d3_before_line EXCEPT SELECT expense_line_id,to_jsonb(l)-'expense_category_id' FROM kkref_expense_line l)
       OR EXISTS(SELECT expense_line_id,to_jsonb(l)-'expense_category_id' FROM kkref_expense_line l EXCEPT SELECT * FROM d3_before_line)
       OR EXISTS(SELECT * FROM d3_before_audit EXCEPT SELECT to_jsonb(a) FROM koiki_audit_event a)
       OR EXISTS(SELECT to_jsonb(a) FROM koiki_audit_event a EXCEPT SELECT * FROM d3_before_audit)
    THEN RAISE EXCEPTION 'Business rows or Audit changed unexpectedly'; END IF;
END $$;
COMMIT;
