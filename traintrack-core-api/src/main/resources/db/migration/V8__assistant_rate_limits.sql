-- Per-user request counter for the AI assistant (Phase 8), enforced here in
-- Postgres rather than in application memory: core-api runs as multiple
-- instances (see V7's ShedLock table), and an in-process counter would let
-- each instance hand out its own independent quota, silently multiplying the
-- real limit by the instance count. window_start is truncated to the
-- configured window size (see AssistantRateLimiter), so each row is one
-- fixed window for one user.
CREATE TABLE assistant_rate_limits (
    user_id       UUID NOT NULL,
    org_id        UUID NOT NULL,
    window_start  TIMESTAMPTZ NOT NULL,
    request_count INT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, window_start)
);
