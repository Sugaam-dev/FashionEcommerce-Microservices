-- V2__seed_initial_admin.sql
-- Seeds default super administrator into users table if not already present

INSERT INTO users (
    id,
    email,
    password_hash,
    first_name,
    last_name,
    phone_number,
    role,
    status,
    auth_provider,
    email_verified,
    failed_login_attempts,
    version,
    created_at,
    updated_at
)
SELECT 
    gen_random_uuid(),
    'admin@fashionecommerce.com',
    crypt('Admin@123', gen_salt('bf', 10)),
    'Super',
    'Admin',
    '+919876543210',
    'ROLE_ADMIN',
    'ACTIVE',
    'LOCAL',
    TRUE,
    0,
    0,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@fashionecommerce.com'
);
