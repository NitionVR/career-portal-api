-- V4__Add_registration_fields.sql

-- Add common fields
ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(50) UNIQUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS contact_number VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE;

-- Add candidate-specific fields
ALTER TABLE users ADD COLUMN IF NOT EXISTS gender VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS race VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS disability VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS alternate_contact_number VARCHAR(20);

-- Add hiring manager-specific fields
ALTER TABLE users ADD COLUMN IF NOT EXISTS company_name VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS industry VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS contact_person VARCHAR(255);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_company_name ON users(company_name);

-- Add check constraint for phone numbers
ALTER TABLE users ADD CONSTRAINT check_contact_number
    CHECK (contact_number ~ '^\\+?[1-9]\\d{1,14}$' OR contact_number IS NULL);
