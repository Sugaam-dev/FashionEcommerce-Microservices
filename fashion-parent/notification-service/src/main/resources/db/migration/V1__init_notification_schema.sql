-- ==============================================================================
-- FLYWAY MIGRATION V1: INITIALIZE NOTIFICATION SERVICE SCHEMA
-- ==============================================================================

CREATE TABLE IF NOT EXISTS notification_logs (
    id UUID PRIMARY KEY,
    recipient VARCHAR(150) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    template_id VARCHAR(100) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    error_message TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ntf_recipient ON notification_logs (recipient);
CREATE INDEX IF NOT EXISTS idx_ntf_status_dt ON notification_logs (status, created_at);

CREATE TABLE IF NOT EXISTS contact_messages (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(20),
    subject VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    admin_reply_notes TEXT,
    replied_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cnt_status_dt ON contact_messages (status, created_at);

CREATE TABLE IF NOT EXISTS email_usage_quotas (
    id UUID PRIMARY KEY,
    date_key VARCHAR(20) NOT NULL UNIQUE,
    count INT NOT NULL DEFAULT 0,
    daily_limit INT NOT NULL DEFAULT 3000,
    last_sent_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_quota_date ON email_usage_quotas (date_key);

CREATE TABLE IF NOT EXISTS store_info_settings (
    id UUID PRIMARY KEY,
    announcement_text TEXT,
    announcement_link VARCHAR(500),
    is_announcement_active BOOLEAN NOT NULL DEFAULT TRUE,
    announcements_json TEXT,
    return_policy_text TEXT,
    delivery_policy_notice TEXT,
    contact_address VARCHAR(500),
    operating_hours VARCHAR(200),
    support_email VARCHAR(150),
    support_phone VARCHAR(20),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
