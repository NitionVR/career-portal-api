-- V3__Create_job_posts_table.sql

CREATE TABLE job_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    company VARCHAR(255),
    job_type VARCHAR(50),
    date_posted VARCHAR(50),
    description TEXT,
    location JSONB,
    remote VARCHAR(50),
    salary VARCHAR(100),
    experience_level VARCHAR(100),
    responsibilities JSONB,
    qualifications JSONB,
    skills JSONB,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',  -- Using VARCHAR instead of enum
    created_by_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_created_by
       FOREIGN KEY(created_by_id)
           REFERENCES users(id)
           ON DELETE SET NULL
);

-- Create indexes for frequently queried columns
CREATE INDEX idx_job_posts_status ON job_posts(status);
CREATE INDEX idx_job_posts_created_by ON job_posts(created_by_id);
CREATE INDEX idx_job_posts_experience_level ON job_posts(experience_level);

-- Create a trigger to automatically update the updated_at timestamp
CREATE TRIGGER update_job_posts_updated_at
BEFORE UPDATE ON job_posts
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();