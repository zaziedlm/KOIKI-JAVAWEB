CREATE TABLE kkref_expense_request (
    expense_request_id uuid PRIMARY KEY,
    applicant_user_id uuid NOT NULL,
    department_id uuid NOT NULL,
    claimed_amount bigint NOT NULL,
    status varchar(20) NOT NULL,
    decision_reason varchar(500),
    version bigint NOT NULL DEFAULT 0,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT ck_kkref_expense_request_amount CHECK (claimed_amount > 0),
    CONSTRAINT ck_kkref_expense_request_status CHECK (
        status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'RETURNED', 'SETTLED')),
    CONSTRAINT ck_kkref_expense_request_reason CHECK (
        decision_reason IS NULL
        OR (decision_reason = btrim(decision_reason) AND decision_reason <> '')),
    CONSTRAINT ck_kkref_expense_request_version CHECK (version >= 0)
);

CREATE TABLE kkref_expense_line (
    expense_line_id uuid PRIMARY KEY,
    expense_request_id uuid NOT NULL,
    expense_category_id uuid NOT NULL,
    usage_date date NOT NULL,
    description varchar(200) NOT NULL,
    purpose varchar(500) NOT NULL,
    amount bigint NOT NULL,
    CONSTRAINT fk_kkref_expense_line_request
        FOREIGN KEY (expense_request_id)
        REFERENCES kkref_expense_request (expense_request_id) ON DELETE CASCADE,
    CONSTRAINT ck_kkref_expense_line_description CHECK (
        description = btrim(description) AND description <> ''),
    CONSTRAINT ck_kkref_expense_line_purpose CHECK (
        purpose = btrim(purpose) AND purpose <> ''),
    CONSTRAINT ck_kkref_expense_line_amount CHECK (amount > 0)
);

CREATE INDEX ix_kkref_expense_request_applicant
    ON kkref_expense_request (applicant_user_id);
CREATE INDEX ix_kkref_expense_request_department_status
    ON kkref_expense_request (department_id, status);
CREATE INDEX ix_kkref_expense_line_request
    ON kkref_expense_line (expense_request_id);

