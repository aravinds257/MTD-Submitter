-- Income records
CREATE TABLE income_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL,
    tax_year_id INTEGER NOT NULL,
    transaction_date DATE NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    income_category VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    source_document VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_income_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT fk_income_tax_year FOREIGN KEY (tax_year_id) REFERENCES tax_years(id),
    CONSTRAINT chk_income_amount CHECK (amount > 0)
);

CREATE INDEX idx_income_business_year ON income_records(business_id, tax_year_id);
CREATE INDEX idx_income_date ON income_records(transaction_date);

-- Expense records
CREATE TABLE expense_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL,
    tax_year_id INTEGER NOT NULL,
    transaction_date DATE NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    expense_category VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    receipt_reference VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_expense_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT fk_expense_tax_year FOREIGN KEY (tax_year_id) REFERENCES tax_years(id),
    CONSTRAINT chk_expense_amount CHECK (amount > 0)
);

CREATE INDEX idx_expense_business_year ON expense_records(business_id, tax_year_id);
CREATE INDEX idx_expense_date ON expense_records(transaction_date);
CREATE INDEX idx_expense_category ON expense_records(expense_category);
