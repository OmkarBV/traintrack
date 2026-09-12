package com.traintrack.coreapi.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.traintrack.coreapi.domain.OutboxEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxRelay outboxRelay;

    @BeforeEach
    void setUp() {
        outboxRelay = new OutboxRelay(outboxEventRepository, kafkaTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void onOutboxEventReadyPublishesAndMarksTheRowPublished() {
        UUID id = UUID.randomUUID();
        OutboxEvent event = new OutboxEvent(id, UUID.randomUUID(), "audit.events", "{}");
        when(outboxEventRepository.findById(id)).thenReturn(Optional.of(event));
        when(kafkaTemplate.send(eq("audit.events"), eq(event.getOrgId().toString()), eq(event.getPayload())))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        outboxRelay.onOutboxEventReady(new OutboxEventReady(id));

        assertThat(event.getPublishedAt()).isNotNull();
        verify(outboxEventRepository).save(event);
    }

    @Test
    void skipsRowsAlreadyPublished() {
        UUID id = UUID.randomUUID();
        OutboxEvent event = new OutboxEvent(id, UUID.randomUUID(), "audit.events", "{}");
        event.markPublished();
        when(outboxEventRepository.findById(id)).thenReturn(Optional.of(event));

        outboxRelay.onOutboxEventReady(new OutboxEventReady(id));

        verify(kafkaTemplate, never()).send(any(), any(), any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sweepPublishesEveryPendingRow() {
        OutboxEvent e1 = new OutboxEvent(UUID.randomUUID(), UUID.randomUUID(), "audit.events", "{}");
        OutboxEvent e2 = new OutboxEvent(UUID.randomUUID(), UUID.randomUUID(), "audit.events", "{}");
        when(outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()).thenReturn(List.of(e1, e2));
        when(outboxEventRepository.findById(e1.getId())).thenReturn(Optional.of(e1));
        when(outboxEventRepository.findById(e2.getId())).thenReturn(Optional.of(e2));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        outboxRelay.sweepUnpublished();

        assertThat(e1.getPublishedAt()).isNotNull();
        assertThat(e2.getPublishedAt()).isNotNull();
    }

    @Test
    void leavesTheRowUnpublishedWhenTheSendFails() {
        UUID id = UUID.randomUUID();
        OutboxEvent event = new OutboxEvent(id, UUID.randomUUID(), "audit.events", "{}");
        when(outboxEventRepository.findById(id)).thenReturn(Optional.of(event));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("boom")));

        outboxRelay.onOutboxEventReady(new OutboxEventReady(id));

        assertThat(event.getPublishedAt()).isNull();
        verify(outboxEventRepository, never()).save(any());
    }
}
