-- Transactional outbox: state-changing service methods insert a row here in
-- the SAME database transaction as the business change, guaranteeing the
-- audit event is durably recorded if (and only if) that change actually
-- committed. OutboxRelay then delivers it to Kafka out-of-band, with
-- eventId (this table's id) doubling as the consumer-side dedup key.
CREATE TABLE outbox_events (
    id           UUID PRIMARY KEY,
    org_id       UUID        NOT NULL,
    payload      TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);
-- Partial index: only unpublished rows are ever queried by the relay's sweep.
CREATE INDEX idx_outbox_events_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;
