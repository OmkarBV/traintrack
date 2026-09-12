package com.traintrack.coreapi.certification.expiry;

import com.traintrack.common.event.CertificationExpiringEvent;
import com.traintrack.coreapi.audit.OutboxWriter;
import com.traintrack.coreapi.domain.Certification;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CertificationExpiryEventPublisher {

    private static final String TOPIC = "certification.expiring";

    private final OutboxWriter outboxWriter;

    public CertificationExpiryEventPublisher(OutboxWriter outboxWriter) {
        this.outboxWriter = outboxWriter;
    }

    public void publishExpiringSoon(Certification certification) {
        CertificationExpiringEvent event = new CertificationExpiringEvent(
                UUID.randomUUID(),
                Instant.now(),
                certification.getOrgId(),
                certification.getId(),
                certification.getUser().getId(),
                certification.getCourse().getId(),
                certification.getExpiresAt());
        outboxWriter.write(event.eventId(), certification.getOrgId(), TOPIC, event);
    }
}
