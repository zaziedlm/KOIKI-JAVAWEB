-- Additional hand-check user. Load only after seed.sql into a fresh dedicated DB.
BEGIN;
INSERT INTO koiki_user(user_id,email,canonical_email,status,version)
 VALUES ('d3000000-0000-0000-0000-000000000006','d3-approver@example.test','d3-approver@example.test','ACTIVE',0);
INSERT INTO koiki_password_credential(user_id,encoded_password,version)
 VALUES ('d3000000-0000-0000-0000-000000000006','{bcrypt}'||crypt('__LOGIN_PASSWORD__',gen_salt('bf',10)),0);
INSERT INTO koiki_role(role_id,role_code,version)
 VALUES ('d3000000-0000-0000-0000-000000000007','D3_APPROVER',0);
INSERT INTO koiki_permission(permission_id,permission_code,version)
 VALUES ('d3000000-0000-0000-0000-000000000008','EXPENSE:APPROVE',0),
        ('d3000000-0000-0000-0000-000000000009','MASTER:ADMIN',0);
INSERT INTO koiki_user_role(user_id,role_id)
 VALUES ('d3000000-0000-0000-0000-000000000006','d3000000-0000-0000-0000-000000000007');
INSERT INTO koiki_role_permission(role_id,permission_id)
 VALUES ('d3000000-0000-0000-0000-000000000007','d3000000-0000-0000-0000-000000000008'),
        ('d3000000-0000-0000-0000-000000000007','d3000000-0000-0000-0000-000000000009');
INSERT INTO kkref_expense_approver_scope(approver_user_id,department_id,created_at)
 VALUES ('d3000000-0000-0000-0000-000000000006','d3000000-0000-4000-8000-000000000004',now());
COMMIT;
