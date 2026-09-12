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
class OutboxWriterTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void writeSavesTheRowUnderTheGivenTopicAndSignalsTheRelay() {
        OutboxWriter writer = new OutboxWriter(outboxEventRepository, applicationEventPublisher, objectMapper);
        UUID eventId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        writer.write(eventId, orgId, "certification.expiring", new SamplePayload("hello"));

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        OutboxEvent saved = outboxCaptor.getValue();
        assertThat(saved.getId()).isEqualTo(eventId);
        assertThat(saved.getOrgId()).isEqualTo(orgId);
        assertThat(saved.getTopic()).isEqualTo("certification.expiring");
        assertThat(saved.getPayload()).contains("hello");

        ArgumentCaptor<OutboxEventReady> readyCaptor = ArgumentCaptor.forClass(OutboxEventReady.class);
        verify(applicationEventPublisher).publishEvent(readyCaptor.capture());
        assertThat(readyCaptor.getValue().outboxEventId()).isEqualTo(eventId);
    }

    private record SamplePayload(String value) {
    }
}
