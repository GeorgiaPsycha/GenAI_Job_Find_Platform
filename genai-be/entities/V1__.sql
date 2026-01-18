CREATE TABLE application
(
    id              UUID                           NOT NULL,
    user_id         UUID                           NOT NULL,
    job_id          UUID                           NOT NULL,
    cv_file_url     TEXT,
    cv_content_text TEXT,
    motivation_text TEXT,
    status          VARCHAR(255) DEFAULT 'APPLIED' NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE    NOT NULL,
    updated_at      TIMESTAMP WITHOUT TIME ZONE    NOT NULL,
    CONSTRAINT pk_application PRIMARY KEY (id)
);

ALTER TABLE application
    ADD CONSTRAINT FK_APPLICATION_ON_JOB FOREIGN KEY (job_id) REFERENCES document (id) ON DELETE CASCADE;

ALTER TABLE application
    ADD CONSTRAINT FK_APPLICATION_ON_USER FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE;