CREATE TABLE kkref_expense_approver_scope (
    approver_user_id uuid NOT NULL,
    department_id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT pk_kkref_expense_approver_scope
        PRIMARY KEY (approver_user_id, department_id)
);

CREATE INDEX ix_kkref_expense_approver_scope_department
    ON kkref_expense_approver_scope (department_id);

