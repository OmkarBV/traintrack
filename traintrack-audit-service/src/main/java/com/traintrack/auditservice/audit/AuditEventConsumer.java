package com.traintrack.auditservice.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traintrack.auditservice.domain.AuditEventRecord;
import com.traintrack.common.event.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * At-least-once delivery means the same message can be redelivered (a
 * rebalance before the offset commits, a retry after a transient failure,
 * ...); {@code event_id}'s unique constraint (the primary key — see V1
 * migration) is what makes a duplicate insert a no-op instead of a second
 * row. Manual acknowledgement (see KafkaConsumerConfig) means the offset
 * only advances after the row is durably written or recognised as a
 * duplicate — never before.
 */
@Component
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    public AuditEventConsumer(AuditEventRepository auditEventRepository, ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "audit.events", groupId = "audit-service")
    public void consume(String payload, Acknowledgment ack) throws Exception {
        AuditEvent event = objectMapper.readValue(payload, AuditEvent.class);
        try {
            auditEventRepository.save(new AuditEventRecord(event));
        } catch (DataIntegrityViolationException e) {
            log.debug("Ignoring duplicate audit event {}", event.eventId());
        }
        ack.acknowledge();
    }
}
