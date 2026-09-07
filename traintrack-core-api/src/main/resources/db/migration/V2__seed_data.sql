-- Local development seed data. Not applied against non-dev environments in a
-- real deployment (Flyway would normally gate this behind a profile-specific
-- migration path); kept simple here since this is a portfolio project.
--
-- Seeded login credentials (all users): password "password123"

INSERT INTO organisations (id, name, created_at)
VALUES ('a0000000-0000-0000-0000-000000000001', 'Acme Compliance Ltd', now());

INSERT INTO roles (id, name)
VALUES ('b0000000-0000-0000-0000-000000000001', 'ADMIN'),
       ('b0000000-0000-0000-0000-000000000002', 'TRAINER'),
       ('b0000000-0000-0000-0000-000000000003', 'EMPLOYEE');

INSERT INTO permissions (id, code)
VALUES ('c0000000-0000-0000-0000-000000000001', 'COURSE_CREATE'),
       ('c0000000-0000-0000-0000-000000000002', 'COURSE_VIEW'),
       ('c0000000-0000-0000-0000-000000000003', 'COURSE_UPDATE'),
       ('c0000000-0000-0000-0000-000000000004', 'COURSE_DELETE'),
       ('c0000000-0000-0000-0000-000000000005', 'ENROLMENT_CREATE'),
       ('c0000000-0000-0000-0000-000000000006', 'ENROLMENT_VIEW_OWN'),
       ('c0000000-0000-0000-0000-000000000007', 'ENROLMENT_VIEW_ALL'),
       ('c0000000-0000-0000-0000-000000000008', 'ENROLMENT_COMPLETE'),
       ('c0000000-0000-0000-0000-000000000009', 'CERT_ISSUE'),
       ('c0000000-0000-0000-0000-00000000000a', 'CERT_VIEW_OWN'),
       ('c0000000-0000-0000-0000-00000000000b', 'CERT_VIEW_ALL'),
       ('c0000000-0000-0000-0000-00000000000c', 'USER_MANAGE');

-- ADMIN: every permission
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT 'b0000000-0000-0000-0000-000000000001', id, now() FROM permissions;

-- TRAINER: manage courses, run enrolments end-to-end, issue certs, see org-wide enrolment/cert data
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT 'b0000000-0000-0000-0000-000000000002', id, now() FROM permissions
WHERE code IN ('COURSE_CREATE', 'COURSE_VIEW', 'COURSE_UPDATE',
               'ENROLMENT_CREATE', 'ENROLMENT_VIEW_ALL', 'ENROLMENT_COMPLETE',
               'CERT_ISSUE', 'CERT_VIEW_ALL');

-- EMPLOYEE: read-only, own data only
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT 'b0000000-0000-0000-0000-000000000003', id, now() FROM permissions
WHERE code IN ('COURSE_VIEW', 'ENROLMENT_VIEW_OWN', 'CERT_VIEW_OWN');

-- password: password123 (BCrypt)
INSERT INTO users (id, org_id, email, password_hash, full_name, status, created_at, updated_at)
VALUES ('d0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001',
        'admin@acme.test', '$2a$10$rX3i8OAcoJqvXmQli.vEkOgq4XGSL9NGwy.8SEfUsZKbuQl1uK7dm',
        'Ada Admin', 'ACTIVE', now(), now()),
       ('d0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001',
        'trainer@acme.test', '$2a$10$rX3i8OAcoJqvXmQli.vEkOgq4XGSL9NGwy.8SEfUsZKbuQl1uK7dm',
        'Tom Trainer', 'ACTIVE', now(), now()),
       ('d0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001',
        'employee@acme.test', '$2a$10$rX3i8OAcoJqvXmQli.vEkOgq4XGSL9NGwy.8SEfUsZKbuQl1uK7dm',
        'Eve Employee', 'ACTIVE', now(), now());

INSERT INTO user_roles (user_id, role_id, created_at)
VALUES ('d0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', now()),
       ('d0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000002', now()),
       ('d0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000003', now());

INSERT INTO courses (id, org_id, title, description, duration_hours, validity_months, status, created_at, updated_at)
VALUES ('e0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001',
        'Fire Safety Awareness', 'Annual mandatory fire safety training.', 4, 12, 'PUBLISHED', now(), now());
