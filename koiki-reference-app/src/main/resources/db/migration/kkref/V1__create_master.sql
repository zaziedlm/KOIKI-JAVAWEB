CREATE TABLE kkref_department (
    department_id uuid PRIMARY KEY,
    department_code varchar(100) NOT NULL,
    department_name varchar(200) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT uk_kkref_department_code UNIQUE (department_code),
    CONSTRAINT ck_kkref_department_code
        CHECK (department_code ~ '^[A-Z][A-Z0-9_]{0,99}$'),
    CONSTRAINT ck_kkref_department_name
        CHECK (department_name = btrim(department_name) AND department_name <> ''),
    CONSTRAINT ck_kkref_department_version CHECK (version >= 0)
);

CREATE TABLE kkref_expense_category (
    expense_category_id uuid PRIMARY KEY,
    expense_category_code varchar(100) NOT NULL,
    expense_category_name varchar(200) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT uk_kkref_expense_category_code UNIQUE (expense_category_code),
    CONSTRAINT ck_kkref_expense_category_code
        CHECK (expense_category_code ~ '^[A-Z][A-Z0-9_]{0,99}$'),
    CONSTRAINT ck_kkref_expense_category_name
        CHECK (expense_category_name = btrim(expense_category_name)
            AND expense_category_name <> ''),
    CONSTRAINT ck_kkref_expense_category_version CHECK (version >= 0)
);

CREATE TABLE kkref_user_department_assignment (
    user_id uuid PRIMARY KEY,
    department_id uuid NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT fk_kkref_user_department_assignment_department
        FOREIGN KEY (department_id)
        REFERENCES kkref_department (department_id) ON DELETE RESTRICT,
    CONSTRAINT ck_kkref_user_department_assignment_version CHECK (version >= 0)
);

CREATE INDEX ix_kkref_user_department_assignment_department
    ON kkref_user_department_assignment (department_id);
