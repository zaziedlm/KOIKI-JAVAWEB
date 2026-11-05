-- Reference-only fixture. Password substituted in memory by verify.py, never committed.
BEGIN;
INSERT INTO koiki_user(user_id,email,canonical_email,status,version)
 VALUES ('d3000000-0000-0000-0000-000000000001','d3-applicant@example.test','d3-applicant@example.test','ACTIVE',0);
INSERT INTO koiki_password_credential(user_id,encoded_password,version)
 VALUES ('d3000000-0000-0000-0000-000000000001','{bcrypt}'||crypt('__LOGIN_PASSWORD__',gen_salt('bf',10)),0);
INSERT INTO koiki_role(role_id,role_code,version) VALUES ('d3000000-0000-0000-0000-000000000002','D3_APPLICANT',0);
INSERT INTO koiki_permission(permission_id,permission_code,version) VALUES ('d3000000-0000-0000-0000-000000000003','EXPENSE:APPLY',0);
INSERT INTO koiki_user_role(user_id,role_id) VALUES ('d3000000-0000-0000-0000-000000000001','d3000000-0000-0000-0000-000000000002');
INSERT INTO koiki_role_permission(role_id,permission_id) VALUES ('d3000000-0000-0000-0000-000000000002','d3000000-0000-0000-0000-000000000003');
INSERT INTO kkref_department(department_id,department_code,department_name,active,version,created_at,updated_at)
 VALUES ('d3000000-0000-4000-8000-000000000004','D3','D3 Department',true,0,now(),now());
INSERT INTO kkref_expense_category(expense_category_id,expense_category_code,expense_category_name,active,version,created_at,updated_at)
 VALUES ('d3000000-0000-4000-8000-000000000005','D3_TRAVEL','D3 Travel',true,0,now(),now());
INSERT INTO kkref_user_department_assignment(user_id,department_id,version,created_at,updated_at)
 VALUES ('d3000000-0000-0000-0000-000000000001','d3000000-0000-4000-8000-000000000004',0,now(),now());
COMMIT;
