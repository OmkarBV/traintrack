-- Backs the Idempotency-Key mechanism on POST /api/v1/enrolments (and any
-- future idempotent POST endpoint). The primary key IS the enforcement
-- mechanism: a concurrent duplicate request fails on insert, not on
-- application-level logic.
CREATE TABLE idempotency_keys (
    id              VARCHAR(255) PRIMARY KEY,
    org_id          UUID         NOT NULL REFERENCES organisations (id),
    endpoint        VARCHAR(255) NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    response_status INTEGER,
    response_body   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_idempotency_keys_org_id ON idempotency_keys (org_id);
