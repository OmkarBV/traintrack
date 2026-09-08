-- Phase 2: authentication support.
--
-- 1. Login only takes an email + password, with no organisation selector, so
--    email must resolve to exactly one user platform-wide. Replace the
--    per-org unique constraint from V1 with a global one.
ALTER TABLE users DROP CONSTRAINT uq_users_org_email;
ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email);

-- 2. Server-side refresh token store. Refresh tokens are themselves signed
--    JWTs (so validating one is a cheap signature check), but each carries a
--    jti that must also exist here, unrevoked and unexpired — this table is
--    what makes logout and rotation-on-reuse-detection possible, which a
--    purely stateless refresh token could never support.
CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

-- 3. A second organisation, purely so integration tests can prove the
--    tenant-isolation mechanism: a user authenticated as an Acme admin must
--    get 404, not 403 or 200, when asking for a Beta Industries user by id.
INSERT INTO organisations (id, name, created_at)
VALUES ('a0000000-0000-0000-0000-000000000002', 'Beta Industries', now());

INSERT INTO users (id, org_id, email, password_hash, full_name, status, created_at, updated_at)
VALUES ('d0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000002',
        'admin@beta.test', '$2a$10$rX3i8OAcoJqvXmQli.vEkOgq4XGSL9NGwy.8SEfUsZKbuQl1uK7dm',
        'Bea Admin', 'ACTIVE', now(), now());

INSERT INTO user_roles (user_id, role_id, created_at)
VALUES ('d0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000001', now());
