-- Fresh isolated koiki_app only. Reruns intentionally fail and roll back.
BEGIN;
DO $$ BEGIN
 IF EXISTS (SELECT 1 FROM koiki_user) OR EXISTS (SELECT 1 FROM kkref_department)
    OR EXISTS (SELECT 1 FROM kkref_expense_request) THEN
   RAISE EXCEPTION 'Seed requires empty Reference fixture';
 END IF;
END $$;
INSERT INTO koiki_user(user_id,email,canonical_email,status,version) VALUES
 ('d3200000-0000-4000-8000-000000000001','sso-a@example.test','sso-a@example.test','ACTIVE',0),
 ('d3200000-0000-4000-8000-000000000002','sso-b@example.test','sso-b@example.test','ACTIVE',0),
 ('d3200000-0000-4000-8000-000000000003','sso-disabled@example.test','sso-disabled@example.test','DISABLED',0),
 ('d3200000-0000-4000-8000-000000000004','sso-unprivileged@example.test','sso-unprivileged@example.test','ACTIVE',0);
INSERT INTO koiki_role(role_id,role_code,version)
 VALUES ('d3200000-0000-4000-8000-000000000011','D3_SSO_APPLICANT',0);
INSERT INTO koiki_permission(permission_id,permission_code,version)
 VALUES ('d3200000-0000-4000-8000-000000000012','EXPENSE:APPLY',0);
INSERT INTO koiki_role_permission(role_id,permission_id)
 VALUES ('d3200000-0000-4000-8000-000000000011','d3200000-0000-4000-8000-000000000012');
INSERT INTO koiki_user_role(user_id,role_id)
 SELECT user_id,'d3200000-0000-4000-8000-000000000011'::uuid FROM koiki_user
 WHERE user_id IN ('d3200000-0000-4000-8000-000000000001','d3200000-0000-4000-8000-000000000002','d3200000-0000-4000-8000-000000000003');
INSERT INTO kkref_department(department_id,department_code,department_name,active,version,created_at,updated_at)
 VALUES ('d3200000-0000-4000-8000-000000000021','D3_SSO','D3 SSO Department',true,0,now(),now());
INSERT INTO kkref_expense_category(expense_category_id,expense_category_code,expense_category_name,active,version,created_at,updated_at)
 VALUES ('d3200000-0000-4000-8000-000000000022','D3_SSO_TRAVEL','D3 SSO Travel',true,0,now(),now());
INSERT INTO kkref_user_department_assignment(user_id,department_id,version,created_at,updated_at)
 SELECT user_id,'d3200000-0000-4000-8000-000000000021'::uuid,0,now(),now() FROM koiki_user;
COMMIT;
