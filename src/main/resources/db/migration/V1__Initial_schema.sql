CREATE TYPE user_role AS ENUM ('CANDIDATE', 'RECRUITER', 'HIRING_MANAGER', 'ADMIN');


CREATE TYPE job_post_status AS ENUM ('DRAFT', 'OPEN', 'CLOSED', 'ARCHIVED');


CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    role user_role NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


CREATE TABLE job_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    status job_post_status NOT NULL DEFAULT 'DRAFT',
    created_by_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_created_by
        FOREIGN KEY(created_by_id)
        REFERENCES users(id)
        ON DELETE SET NULL
);


CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_job_posts_status ON job_posts(status);
CREATE INDEX idx_job_posts_created_by ON job_posts(created_by_id);


CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';


CREATE TRIGGER update_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();


CREATE TRIGGER update_job_posts_updated_at
BEFORE UPDATE ON job_posts
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
