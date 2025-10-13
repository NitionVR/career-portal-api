CREATE TABLE job_application_audit (
    id UUID PRIMARY KEY,
    job_application_id UUID NOT NULL,
    status VARCHAR(255) NOT NULL,
    date TIMESTAMP NOT NULL,
    message TEXT,
    CONSTRAINT fk_job_application FOREIGN KEY (job_application_id) REFERENCES job_applications(id)
);
