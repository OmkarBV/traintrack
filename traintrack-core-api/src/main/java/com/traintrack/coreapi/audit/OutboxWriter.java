package com.traintrack.coreapi.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traintrack.coreapi.domain.OutboxEvent;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Shared by {@link AuditPublisher} and any other event source: writes an
 * outbox row in the caller's ongoing transaction (atomic with whatever
 * business change triggered it), then asks {@link OutboxRelay} to attempt an
 * immediate Kafka publish once that transaction commits. Not
 * audit-event-specific — {@code topic} and the serialised payload are
 * entirely up to the caller.
 */
@Component
public class OutboxWriter {

    private final OutboxEventRepository outboxEventRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    public OutboxWriter(
            OutboxEventRepository outboxEventRepository, ApplicationEventPublisher applicationEventPublisher, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.objectMapper = objectMapper;
    }

    public void write(UUID eventId, UUID orgId, String topic, Object event) {
        outboxEventRepository.save(new OutboxEvent(eventId, orgId, topic, serialize(event)));
        applicationEventPublisher.publishEvent(new OutboxEventReady(eventId));
    }

    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise event for topic publish", e);
        }
    }
}
