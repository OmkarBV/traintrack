package com.traintrack.coreapi.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traintrack.coreapi.domain.OutboxEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AuditPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void recordWritesAnOutboxRowAndSignalsTheRelay() {
        UUID orgId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        AuditPublisher publisher =
                new AuditPublisher(outboxEventRepository, applicationEventPublisher, objectMapper, "traintrack-core-api");

        publisher.record(orgId, actorId, "Course", entityId, "COURSE_CREATED", "Course 'X' created");

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        OutboxEvent saved = outboxCaptor.getValue();
        assertThat(saved.getOrgId()).isEqualTo(orgId);
        assertThat(saved.getPayload()).contains("COURSE_CREATED").contains(entityId.toString()).contains("traintrack-core-api");

        ArgumentCaptor<OutboxEventReady> eventCaptor = ArgumentCaptor.forClass(OutboxEventReady.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().outboxEventId()).isEqualTo(saved.getId());
    }
}
