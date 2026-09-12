-- ShedLock's required schema (column names/types are fixed by the library's
-- JdbcTemplateLockProvider) — ensures a cron job runs on only one app
-- instance at a time in a horizontally-scaled deployment.
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at  TIMESTAMP(3) NOT NULL,
    locked_by  VARCHAR(255) NOT NULL
);

-- Generalises the outbox from "always audit.events" to "any topic", so the
-- new certification-expiry job (and anything future) can reuse the same
-- transactional-outbox guarantee OutboxRelay already provides, rather than
-- duplicating that machinery per event type.
ALTER TABLE outbox_events ADD COLUMN topic VARCHAR(255) NOT NULL DEFAULT 'audit.events';
ALTER TABLE outbox_events ALTER COLUMN topic DROP DEFAULT;

-- Tracks whether a certification has already had its "expiring soon" event
-- published, so the daily job is genuinely idempotent: re-running it (or it
-- simply running again tomorrow, while the certification is still within the
-- 30-day window) does not re-publish for a certification already notified.
ALTER TABLE certifications ADD COLUMN expiring_notified_at TIMESTAMPTZ;
