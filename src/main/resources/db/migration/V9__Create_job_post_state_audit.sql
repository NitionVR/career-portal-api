-- Create job_post_state_audit table
CREATE TABLE job_post_state_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_post_id UUID NOT NULL,
    from_status VARCHAR(50) NOT NULL,
    to_status VARCHAR(50) NOT NULL,
    changed_by_id UUID NOT NULL,
    reason TEXT,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_audit_job_post
        FOREIGN KEY(job_post_id)
            REFERENCES job_posts(id)
            ON DELETE CASCADE,
    CONSTRAINT fk_audit_changed_by
        FOREIGN KEY(changed_by_id)
            REFERENCES users(id)
            ON DELETE SET NULL
);

-- Create indexes for efficient querying
CREATE INDEX idx_state_audit_job_post ON job_post_state_audit(job_post_id);
CREATE INDEX idx_state_audit_changed_at ON job_post_state_audit(changed_at DESC);
CREATE INDEX idx_state_audit_changed_by ON job_post_state_audit(changed_by_id);
CREATE INDEX idx_state_audit_statuses ON job_post_state_audit(from_status, to_status);

-- Add comment
COMMENT ON TABLE job_post_state_audit IS 'Audit trail for all job post state transitions';
