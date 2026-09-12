package com.traintrack.coreapi.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.traintrack.common.event.AuditEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditPublisherTest {

    @Mock
    private OutboxWriter outboxWriter;

    @Test
    void recordBuildsAnAuditEventAndDelegatesToTheOutboxWriter() {
        UUID orgId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        AuditPublisher publisher = new AuditPublisher(outboxWriter, "traintrack-core-api");

        publisher.record(orgId, actorId, "Course", entityId, "COURSE_CREATED", "Course 'X' created");

        ArgumentCaptor<UUID> eventIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<AuditEvent> eventCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(outboxWriter).write(eventIdCaptor.capture(), eq(orgId), eq("audit.events"), eventCaptor.capture());

        AuditEvent event = eventCaptor.getValue();
        assertThat(event.eventId()).isEqualTo(eventIdCaptor.getValue());
        assertThat(event.orgId()).isEqualTo(orgId);
        assertThat(event.actorUserId()).isEqualTo(actorId);
        assertThat(event.entityType()).isEqualTo("Course");
        assertThat(event.entityId()).isEqualTo(entityId.toString());
        assertThat(event.action()).isEqualTo("COURSE_CREATED");
        assertThat(event.sourceApplication()).isEqualTo("traintrack-core-api");
    }
}
