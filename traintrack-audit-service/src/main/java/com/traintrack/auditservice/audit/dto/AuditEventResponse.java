package com.traintrack.auditservice.audit.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
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
