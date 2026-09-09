package com.traintrack.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * The wire contract for the {@code audit.events} Kafka topic, shared between
 * core-api (producer) and audit-service (consumer) so both sides serialize
 * and deserialize the exact same shape without duplicating it.
 *
 * <p>{@code eventId} is the deduplication key on the consumer side — Kafka's
 * at-least-once delivery means the same event can arrive more than once, and
 * {@code eventId} is what lets audit-service tell "already recorded this"
 * apart from "new event". {@code entityId} is a String rather than UUID
 * since this contract shouldn't have to change if a future entity type uses
 * a different id scheme.
 */
public record AuditEvent(
        UUID eventId,
        Instant occurredAt,
        String sourceApplication,
        UUID orgId,
        UUID actorUserId,
        String entityType,
        String entityId,
        String action,
        String changeSummary) {
}
