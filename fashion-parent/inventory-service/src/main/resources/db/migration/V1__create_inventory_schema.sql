-- ==============================================================================
-- INVENTORY SERVICE — V1 FLYWAY SCHEMA MIGRATION
-- Database: inventory_db | Engine: PostgreSQL 16+
-- ==============================================================================

-- 1. Inventory Items Table
CREATE TABLE IF NOT EXISTS inventory_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku VARCHAR(100) NOT NULL UNIQUE,
    product_id VARCHAR(100) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    available_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    low_stock_threshold INT NOT NULL DEFAULT 5,
    warehouse_code VARCHAR(50) NOT NULL DEFAULT 'WH-MAIN',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_inv_avail_qty_non_negative CHECK (available_quantity >= 0),
    CONSTRAINT chk_inv_res_qty_non_negative CHECK (reserved_quantity >= 0)
);

CREATE INDEX IF NOT EXISTS idx_inv_sku ON inventory_items(sku);
CREATE INDEX IF NOT EXISTS idx_inv_product ON inventory_items(product_id);
CREATE INDEX IF NOT EXISTS idx_inv_low_stock ON inventory_items(available_quantity, low_stock_threshold);

-- 2. Stock Reservations Table
CREATE TABLE IF NOT EXISTS stock_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id VARCHAR(100) NOT NULL,
    sku VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_res_qty_positive CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_res_order_status ON stock_reservations(order_id, status);
CREATE INDEX IF NOT EXISTS idx_res_expiry ON stock_reservations(status, expires_at);
CREATE INDEX IF NOT EXISTS idx_res_sku ON stock_reservations(sku);

-- 3. Outbox Events Table (Part B: Outbox Pattern)
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_inv_outbox_status_created ON outbox_events(status, created_at);
CREATE INDEX IF NOT EXISTS idx_inv_outbox_aggregate ON outbox_events(aggregate_type, aggregate_id);
