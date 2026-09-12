package com.traintrack.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to {@code certification.expiring} once per certification, the
 * first time a daily job finds it within the expiry-warning window — not
 * once per day it remains in that window. No consumer for this topic is
 * built in this project (none was specified); it exists to demonstrate the
 * producer side of the same outbox-backed publishing path audit events use.
 */
public record CertificationExpiringEvent(
        UUID eventId, Instant occurredAt, UUID orgId, UUID certificationId, UUID userId, UUID courseId, Instant expiresAt) {
}
