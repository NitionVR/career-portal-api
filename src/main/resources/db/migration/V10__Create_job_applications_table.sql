CREATE TABLE job_applications (
    id UUID PRIMARY KEY,
    job_post_id UUID NOT NULL,
    candidate_id UUID NOT NULL,
    status VARCHAR(255) NOT NULL,
    application_date TIMESTAMP NOT NULL,
    CONSTRAINT fk_job_post FOREIGN KEY (job_post_id) REFERENCES job_posts(id),
    CONSTRAINT fk_candidate FOREIGN KEY (candidate_id) REFERENCES users(id)
);
