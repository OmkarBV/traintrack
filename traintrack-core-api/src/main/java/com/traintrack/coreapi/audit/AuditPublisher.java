package com.traintrack.coreapi.audit;

import com.traintrack.common.event.AuditEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * The one place state-changing service methods go to record an audit event.
 * Deliberately explicit call sites (one per meaningful business action)
 * rather than generic AOP around every {@code @Transactional} method: a
 * blanket interceptor can't produce a meaningful {@code changeSummary}
 * without deep entity-diffing, and would audit incidental writes alongside
 * the business actions actually worth recording.
 */
@Service
public class AuditPublisher {

    private static final String TOPIC = "audit.events";

    private final OutboxWriter outboxWriter;
    private final String sourceApplication;

    public AuditPublisher(OutboxWriter outboxWriter, @Value("${spring.application.name}") String sourceApplication) {
        this.outboxWriter = outboxWriter;
        this.sourceApplication = sourceApplication;
    }

    public void record(UUID orgId, UUID actorUserId, String entityType, UUID entityId, String action, String changeSummary) {
        AuditEvent event = new AuditEvent(
                UUID.randomUUID(),
                Instant.now(),
                sourceApplication,
                orgId,
                actorUserId,
                entityType,
                entityId.toString(),
                action,
                changeSummary);
        outboxWriter.write(event.eventId(), orgId, TOPIC, event);
    }
}
