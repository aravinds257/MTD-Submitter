-- Quarterly obligation periods
CREATE TABLE quarterly_periods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL,
    tax_year_id INTEGER NOT NULL,
    quarter_number INTEGER NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    hmrc_obligation_id VARCHAR(255),
    submitted_at TIMESTAMP,
    hmrc_submission_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_qp_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT fk_qp_tax_year FOREIGN KEY (tax_year_id) REFERENCES tax_years(id),
    CONSTRAINT chk_quarter_number CHECK (quarter_number BETWEEN 1 AND 4),
    CONSTRAINT uk_qp_business_year_quarter UNIQUE (business_id, tax_year_id, quarter_number)
);

CREATE INDEX idx_qp_business_year ON quarterly_periods(business_id, tax_year_id);
CREATE INDEX idx_qp_due_date ON quarterly_periods(due_date);

-- Quarterly submission records
CREATE TABLE quarterly_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quarterly_period_id UUID NOT NULL,
    submission_json JSONB,
    response_json JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    submitted_at TIMESTAMP,
    hmrc_submission_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_qs_quarterly_period FOREIGN KEY (quarterly_period_id) REFERENCES quarterly_periods(id) ON DELETE CASCADE
);

CREATE INDEX idx_qs_period ON quarterly_submissions(quarterly_period_id);
