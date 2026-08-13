CREATE TABLE ws_category (
    category_id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE TABLE ws_expense_request (
    expense_request_id UUID PRIMARY KEY,
    category_id UUID NOT NULL,
    description VARCHAR(500) NOT NULL,
    requested_amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT fk_ws_expense_request_category
        FOREIGN KEY (category_id) REFERENCES ws_category (category_id),
    CONSTRAINT ck_ws_expense_request_amount_positive
        CHECK (requested_amount > 0),
    CONSTRAINT ck_ws_expense_request_status
        CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED'))
);

CREATE TABLE ws_expense_line (
    expense_line_id UUID PRIMARY KEY,
    expense_request_id UUID NOT NULL,
    description VARCHAR(500) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    line_order INTEGER NOT NULL,
    CONSTRAINT fk_ws_expense_line_request
        FOREIGN KEY (expense_request_id)
        REFERENCES ws_expense_request (expense_request_id)
        ON DELETE CASCADE,
    CONSTRAINT ck_ws_expense_line_amount_positive
        CHECK (amount > 0),
    CONSTRAINT uq_ws_expense_line_order
        UNIQUE (expense_request_id, line_order)
);

CREATE INDEX ix_ws_expense_request_category_status
    ON ws_expense_request (category_id, status);
