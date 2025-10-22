-- V[version]__add_applicant_search_indexes.sql

-- GIN indexes for JSONB columns (fast containment and path queries)
CREATE INDEX IF NOT EXISTS idx_user_profile_gin
    ON users USING GIN (profile jsonb_path_ops);

CREATE INDEX IF NOT EXISTS idx_job_post_skills_gin
    ON job_posts USING GIN (skills jsonb_path_ops);

CREATE INDEX IF NOT EXISTS idx_job_post_location_gin
    ON job_posts USING GIN (location jsonb_path_ops);

-- B-tree indexes for frequently filtered columns
CREATE INDEX IF NOT EXISTS idx_job_application_org_status
    ON job_applications(job_post_id, status, application_date DESC)
    WHERE status NOT IN ('WITHDRAWN', 'REJECTED');

CREATE INDEX IF NOT EXISTS idx_job_post_org
    ON job_posts(organization_id, status)
    WHERE status = 'OPEN';

-- Composite index for candidate lookups
CREATE INDEX IF NOT EXISTS idx_user_name_search
    ON users(LOWER(first_name), LOWER(last_name))
    WHERE role = 'CANDIDATE';

-- Index for experience years extraction (PostgreSQL expression index)
CREATE INDEX IF NOT EXISTS idx_user_profile_experience
    ON users((CAST(profile->>'experienceYears' AS INTEGER)))
    WHERE profile ? 'experienceYears' AND role = 'CANDIDATE';

-- Index for application date sorting
CREATE INDEX IF NOT EXISTS idx_job_application_date
    ON job_applications(application_date DESC);

-- Covering index for common queries
CREATE INDEX IF NOT EXISTS idx_job_application_covering
    ON job_applications(job_post_id, candidate_id, status, application_date DESC, viewed_by_employer);

-- Comment on indexes for documentation
COMMENT ON INDEX idx_user_profile_gin IS
    'GIN index for fast JSONB queries on user profile data';
COMMENT ON INDEX idx_job_application_org_status IS
    'Composite index for filtering applications by organization and status';
COMMENT ON INDEX idx_user_profile_experience IS
    'Expression index for filtering by experience years from JSONB';
