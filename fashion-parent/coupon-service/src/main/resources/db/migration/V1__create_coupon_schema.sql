-- Schema Migration V1 for Coupon Service

CREATE TABLE IF NOT EXISTS coupons (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    description TEXT,
    discount_type VARCHAR(30) NOT NULL,
    discount_value NUMERIC(10, 2) NOT NULL,
    max_discount_amount NUMERIC(10, 2),
    min_order_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    usage_limit_total INT NOT NULL DEFAULT 0,
    usage_limit_per_user INT NOT NULL DEFAULT 1,
    times_used INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_coupon_code UNIQUE (code)
);

CREATE INDEX IF NOT EXISTS idx_cpn_code ON coupons(code);
CREATE INDEX IF NOT EXISTS idx_cpn_active ON coupons(is_active, starts_at, expires_at);

CREATE TABLE IF NOT EXISTS coupon_usages (
    id UUID PRIMARY KEY,
    coupon_id UUID NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    order_id VARCHAR(100) NOT NULL,
    discount_amount NUMERIC(10, 2) NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_usg_coupon_order UNIQUE (coupon_id, order_id)
);

CREATE INDEX IF NOT EXISTS idx_usg_coupon_user ON coupon_usages(coupon_id, user_id);
CREATE INDEX IF NOT EXISTS idx_usg_coupon_order ON coupon_usages(coupon_id, order_id);

CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_created ON outbox_events(status, created_at);
