package com.traintrack.auditservice.audit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traintrack.auditservice.domain.AuditEventRecord;
import com.traintrack.common.event.AuditEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.support.Acknowledgment;

@ExtendWith(MockitoExtension.class)
class AuditEventConsumerTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private Acknowledgment acknowledgment;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private AuditEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AuditEventConsumer(auditEventRepository, objectMapper);
    }

    @Test
    void storesANewEventAndAcknowledges() throws Exception {
        String payload = objectMapper.writeValueAsString(sampleEvent());

        consumer.consume(payload, acknowledgment);

        verify(auditEventRepository).save(any(AuditEventRecord.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void ignoresADuplicateDeliveryButStillAcknowledges() throws Exception {
        String payload = objectMapper.writeValueAsString(sampleEvent());
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(auditEventRepository)
                .save(any(AuditEventRecord.class));

        consumer.consume(payload, acknowledgment);

        // A duplicate is expected under at-least-once delivery, not a processing
        // failure — the offset must still advance so the consumer doesn't get
        // stuck retrying a message it has already durably recorded.
        verify(acknowledgment).acknowledge();
    }

    @Test
    void malformedPayloadPropagatesAndDoesNotAcknowledge() {
        assertThatThrownBy(() -> consumer.consume("not valid json", acknowledgment)).isInstanceOf(Exception.class);

        // Left unacknowledged so the container's error handler (retry, then
        // DLT) sees it, not this test's job to assert that infrastructure.
        verify(acknowledgment, never()).acknowledge();
    }

    private AuditEvent sampleEvent() {
        return new AuditEvent(
                UUID.randomUUID(),
                Instant.now(),
                "traintrack-core-api",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Course",
                UUID.randomUUID().toString(),
                "COURSE_CREATED",
                "Course 'X' created");
    }
}
