-- V6__Fix_phone_number_constraint.sql

-- Drop the existing constraint with incorrect regex
ALTER TABLE users DROP CONSTRAINT IF EXISTS check_contact_number;

-- Add the corrected constraint with proper regex
ALTER TABLE users ADD CONSTRAINT check_contact_number
    CHECK(contact_number ~ '^\+?[1-9][0-9]{1,14}$' OR contact_number is NULL)