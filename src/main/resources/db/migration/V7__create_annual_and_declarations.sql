-- Annual adjustments (year-end)
CREATE TABLE annual_adjustments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL,
    tax_year_id INTEGER NOT NULL,
    adjustment_type VARCHAR(100) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_aa_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT fk_aa_tax_year FOREIGN KEY (tax_year_id) REFERENCES tax_years(id)
);

CREATE INDEX idx_aa_business_year ON annual_adjustments(business_id, tax_year_id);

-- Final declarations (annual crystallisation)
CREATE TABLE final_declarations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    tax_year_id INTEGER NOT NULL,
    calculation_id VARCHAR(255),
    total_income DECIMAL(12, 2),
    total_expenses DECIMAL(12, 2),
    total_tax_due DECIMAL(12, 2),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    submitted_at TIMESTAMP,
    response_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_fd_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_fd_tax_year FOREIGN KEY (tax_year_id) REFERENCES tax_years(id),
    CONSTRAINT uk_fd_user_year UNIQUE (user_id, tax_year_id)
);
