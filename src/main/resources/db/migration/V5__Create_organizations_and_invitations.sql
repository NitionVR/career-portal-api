-- V5__Create_organizations_and_invitations.sql

-- Create organizations table
CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) UNIQUE NOT NULL,
    industry VARCHAR(100),
    description TEXT,
    website VARCHAR(255),
    created_by_id UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Add organization relationship to users
ALTER TABLE users
ADD COLUMN organization_id UUID REFERENCES organizations(id),
ADD COLUMN invited_by_id UUID REFERENCES users(id),
ADD COLUMN invited_at TIMESTAMPTZ;

-- Create invitation status enum
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'invitation_status') THEN
        CREATE TYPE invitation_status AS ENUM ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED');
    END IF;
END$$;


-- Create recruiter_invitations table
CREATE TABLE recruiter_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    token VARCHAR(255) UNIQUE NOT NULL,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    invited_by_id UUID NOT NULL REFERENCES users(id),
    status invitation_status NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    CONSTRAINT unique_pending_invitation UNIQUE (email, organization_id, status)
);

-- Create indexes
CREATE INDEX idx_organizations_name ON organizations(name);
CREATE INDEX idx_users_organization ON users(organization_id);
CREATE INDEX idx_invitations_token ON recruiter_invitations(token);
CREATE INDEX idx_invitations_email ON recruiter_invitations(email);
CREATE INDEX idx_invitations_status ON recruiter_invitations(status);
CREATE INDEX idx_invitations_expires ON recruiter_invitations(expires_at);

-- Add trigger for organizations
CREATE TRIGGER update_organizations_updated_at
BEFORE UPDATE ON organizations
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
