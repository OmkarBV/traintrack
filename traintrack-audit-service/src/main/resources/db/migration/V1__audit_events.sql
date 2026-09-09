-- event_id (the producer's id, not a locally-generated one) is the primary
-- key: Kafka's at-least-once delivery means the same message can arrive more
-- than once, and this constraint is what makes re-inserting a duplicate a
-- no-op rather than a second row.
CREATE TABLE audit_events (
    event_id           UUID PRIMARY KEY,
    occurred_at        TIMESTAMPTZ  NOT NULL,
    received_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    source_application VARCHAR(100) NOT NULL,
    org_id             UUID         NOT NULL,
    actor_user_id      UUID,
    entity_type        VARCHAR(100) NOT NULL,
    entity_id          VARCHAR(100) NOT NULL,
    action              VARCHAR(100) NOT NULL,
    change_summary      TEXT
);
CREATE INDEX idx_audit_events_org_entity_type ON audit_events (org_id, entity_type);
CREATE INDEX idx_audit_events_occurred_at ON audit_events (occurred_at);
