package com.traintrack.coreapi.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traintrack.common.event.AuditEvent;
import com.traintrack.coreapi.domain.OutboxEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * The one place state-changing service methods go to record an audit event.
 * Deliberately explicit call sites (one per meaningful business action)
 * rather than generic AOP around every {@code @Transactional} method: a
 * blanket interceptor can't produce a meaningful {@code changeSummary}
 * without deep entity-diffing, and would audit incidental writes alongside
 * the business actions actually worth recording.
 *
 * <p>Writes to the outbox table in the caller's ongoing transaction — so the
 * audit record is atomic with whatever business change triggered it — then
 * asks {@link OutboxRelay} to attempt an immediate Kafka publish once that
 * transaction actually commits.
 */
@Service
public class AuditPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;
    private final String sourceApplication;

    public AuditPublisher(
            OutboxEventRepository outboxEventRepository,
            ApplicationEventPublisher applicationEventPublisher,
            ObjectMapper objectMapper,
            @Value("${spring.application.name}") String sourceApplication) {
        this.outboxEventRepository = outboxEventRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.objectMapper = objectMapper;
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
        outboxEventRepository.save(new OutboxEvent(event.eventId(), orgId, serialize(event)));
        applicationEventPublisher.publishEvent(new OutboxEventReady(event.eventId()));
    }

    private String serialize(AuditEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise audit event", e);
        }
    }
}
