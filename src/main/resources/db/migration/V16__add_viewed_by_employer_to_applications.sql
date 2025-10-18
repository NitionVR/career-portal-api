ALTER TABLE job_applications
ADD COLUMN viewed_by_employer BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_job_applications_viewed ON job_applications(viewed_by_employer);
