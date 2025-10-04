-- V7__Fix_invitation_status_enum.sql

-- First, create a temporary column
ALTER TABLE recruiter_invitations ADD COLUMN status_temp VARCHAR(50);

-- Copy the existing data
UPDATE recruiter_invitations SET status_temp = status::text;

-- Drop the old column
ALTER TABLE recruiter_invitations DROP COLUMN status;

-- Rename the temporary column
ALTER TABLE recruiter_invitations RENAME COLUMN status_temp TO status;

-- Add NOT NULL constraint and default
ALTER TABLE recruiter_invitations ALTER COLUMN status SET NOT NULL;
ALTER TABLE recruiter_invitations ALTER COLUMN status SET DEFAULT 'PENDING';

-- Add check constraint to ensure valid values
ALTER TABLE recruiter_invitations ADD CONSTRAINT check_invitation_status
    CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED'));

-- Drop the unused enum type
DROP TYPE IF EXISTS invitation_status;