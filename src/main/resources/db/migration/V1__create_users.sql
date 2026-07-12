-- Users table - core user accounts
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    nino_encrypted VARCHAR(512),
    utr_encrypted VARCHAR(512),
    hmrc_user_id VARCHAR(255),
    stripe_customer_id VARCHAR(255),
    subscription_status VARCHAR(50) NOT NULL DEFAULT 'TRIAL',
    trial_end_date TIMESTAMP,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_stripe_customer_id ON users(stripe_customer_id);
