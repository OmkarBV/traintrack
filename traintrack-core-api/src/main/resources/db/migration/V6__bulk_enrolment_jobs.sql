CREATE TABLE bulk_enrolment_jobs (
    id           UUID PRIMARY KEY,
    org_id       UUID        NOT NULL REFERENCES organisations (id),
    status       VARCHAR(20) NOT NULL,
    total_rows   INTEGER     NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    created_by   UUID
);
CREATE INDEX idx_bulk_enrolment_jobs_org_id ON bulk_enrolment_jobs (org_id);

-- No surrogate id: (job_id, row_number) is already a natural, meaningful key,
-- and each row is written exactly once by exactly one worker — there's no
-- redelivery/dedup concern here the way there is for Kafka-consumed rows.
CREATE TABLE bulk_enrolment_job_rows (
    job_id        UUID         NOT NULL REFERENCES bulk_enrolment_jobs (id),
    row_number    INTEGER      NOT NULL,
    raw_user_id   VARCHAR(255) NOT NULL,
    raw_course_id VARCHAR(255) NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    enrolment_id  UUID,
    error_message TEXT,
    PRIMARY KEY (job_id, row_number)
);
