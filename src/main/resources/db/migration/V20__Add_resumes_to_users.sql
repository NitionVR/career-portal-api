-- V20__Add_resumes_to_users.sql

ALTER TABLE users ADD COLUMN resumes jsonb;

COMMENT ON COLUMN users.resumes IS 'JSONB array of candidate resumes';
