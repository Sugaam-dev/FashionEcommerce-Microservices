-- =============================================================================
-- PAYMENT SERVICE FLYWAY MIGRATION: V1__create_payment_schema.sql
-- =============================================================================

CREATE TABLE IF NOT EXISTS payment_transactions (
    id UUID PRIMARY KEY,
    order_id VARCHAR(100) NOT NULL,
    user_id UUID NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    payment_method VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'INITIATED',
    gateway_order_id VARCHAR(100),
    gateway_payment_id VARCHAR(100),
    gateway_signature VARCHAR(255),
    manual_upi_utr_number VARCHAR(50),
    manual_upi_proof_image_url VARCHAR(500),
    failure_reason VARCHAR(500),
    client_ip VARCHAR(64),
    verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tx_order ON payment_transactions (order_id);
CREATE INDEX IF NOT EXISTS idx_tx_status ON payment_transactions (status);
CREATE INDEX IF NOT EXISTS idx_tx_gateway_ref ON payment_transactions (gateway_payment_id);
CREATE INDEX IF NOT EXISTS idx_tx_user ON payment_transactions (user_id);

CREATE TABLE IF NOT EXISTS payment_gateway_settings (
    id UUID PRIMARY KEY,
    razorpay_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    razorpay_key_id VARCHAR(100),
    manual_upi_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    upi_id VARCHAR(100),
    upi_account_holder VARCHAR(100),
    upi_qr_code_url VARCHAR(500),
    bank_name VARCHAR(100),
    bank_account_number VARCHAR(100),
    bank_ifsc_code VARCHAR(20),
    bank_branch VARCHAR(100),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed default single-row payment gateway settings
INSERT INTO payment_gateway_settings (
    id, razorpay_enabled, razorpay_key_id, manual_upi_enabled,
    upi_id, upi_account_holder, upi_qr_code_url,
    bank_name, bank_account_number, bank_ifsc_code, bank_branch, updated_at
) VALUES (
    'a1b2c3d4-e5f6-4a5b-8c9d-0e1f2a3b4c5d',
    TRUE,
    'rzp_test_public_key',
    TRUE,
    'store@upi',
    'Fashion Store Private Limited',
    'https://cdn.fashionstore.com/qr/upi-payment-qr.png',
    'HDFC Bank',
    '50100123456789',
    'HDFC0001234',
    'Indiranagar Branch',
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    error_message VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_payment_outbox_status_created ON outbox_events (status, created_at);

CREATE TABLE IF NOT EXISTS processed_events (
    event_id VARCHAR(100) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
