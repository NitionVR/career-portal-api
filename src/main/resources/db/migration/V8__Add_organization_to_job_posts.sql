-- Add organization_id column to job_posts
ALTER TABLE job_posts ADD COLUMN organization_id UUID;

-- Add foreign key constraint
ALTER TABLE job_posts ADD CONSTRAINT fk_job_posts_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id);

-- Set organization_id based on the creator's organization
UPDATE job_posts jp
SET organization_id = u.organization_id
FROM users u
WHERE jp.created_by_id = u.id AND u.organization_id IS NOT NULL;

-- Create index for better query performance
CREATE INDEX idx_job_posts_organization ON job_posts(organization_id);

-- Add constraint: job posts must have an organization (after data migration)
-- We'll add this in a comment for now since existing posts might not have it
-- ALTER TABLE job_posts ALTER COLUMN organization_id SET NOT NULL;