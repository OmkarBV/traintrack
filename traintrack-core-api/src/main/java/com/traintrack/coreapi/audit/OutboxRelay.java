package com.traintrack.coreapi.audit;

import com.traintrack.coreapi.domain.OutboxEvent;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Delivers outbox rows to the {@code audit.events} topic, partitioned by
 * orgId so events for one organisation stay ordered.
 *
 * <p>Two delivery paths, deliberately combined:
 * <ul>
 *   <li>{@link #onOutboxEventReady} — a best-effort, low-latency attempt
 *       right after the originating transaction commits.</li>
 *   <li>{@link #sweepUnpublished} — a scheduled safety net that catches
 *       anything the immediate attempt missed: the process crashing between
 *       commit and that callback, or a transient Kafka failure.</li>
 * </ul>
 * Both call the same idempotent publish path, and both are safe to run
 * concurrently across multiple app instances without any coordination
 * (unlike the scheduled jobs Phase 6 adds, which need ShedLock because they
 * mutate business data) — audit-service dedupes on eventId, so a row
 * published twice by two racing attempts is harmless, not a duplicate audit
 * entry.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final String TOPIC = "audit.events";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelay(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutboxEventReady(OutboxEventReady event) {
        publishIfUnpublished(event.outboxEventId());
    }

    @Scheduled(fixedDelay = 5000)
    public void sweepUnpublished() {
        List<OutboxEvent> pending = outboxEventRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
        pending.forEach(e -> publishIfUnpublished(e.getId()));
    }

    private void publishIfUnpublished(UUID outboxEventId) {
        outboxEventRepository.findById(outboxEventId).ifPresent(e -> {
            if (e.getPublishedAt() != null) {
                return;
            }
            try {
                kafkaTemplate.send(TOPIC, e.getOrgId().toString(), e.getPayload()).get(5, TimeUnit.SECONDS);
                e.markPublished();
                outboxEventRepository.save(e);
            } catch (Exception ex) {
                log.warn("Failed to publish outbox event {}, will retry via the sweep", outboxEventId, ex);
            }
        });
    }
}
