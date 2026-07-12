-- Business income sources (self-employment, property)
CREATE TABLE businesses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    hmrc_business_id VARCHAR(255),
    business_type VARCHAR(50) NOT NULL,
    trading_name VARCHAR(255),
    description TEXT,
    address_json JSONB,
    accounting_type VARCHAR(20) NOT NULL DEFAULT 'CASH',
    commencement_date DATE,
    cessation_date DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_businesses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_businesses_hmrc UNIQUE (user_id, hmrc_business_id)
);

CREATE INDEX idx_businesses_user ON businesses(user_id);
CREATE INDEX idx_businesses_type ON businesses(business_type);
