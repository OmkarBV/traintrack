package com.traintrack.coreapi.audit;

import com.traintrack.coreapi.domain.OutboxEvent;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Delivers outbox rows to whichever Kafka topic each row names (see
 * {@code OutboxEvent.topic}) — generic across event types, not
 * audit-events-specific — partitioned by orgId so events for one
 * organisation stay ordered within a topic.
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
 * mutate business data) — a row published twice by two racing attempts is
 * harmless as long as the consumer on the other end dedupes on eventId, the
 * way audit-service does for audit.events.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Event IDs with a send currently awaiting Kafka's ack. Guards against
     * the 5-second sweep re-issuing a send for a row whose previous attempt
     * hasn't completed yet — without this, a slow or unreachable broker
     * would let the number of concurrent in-flight sends for one row grow
     * without bound instead of just retrying every 5 seconds as intended.
     */
    private final Set<UUID> inFlight = ConcurrentHashMap.newKeySet();

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
        if (!inFlight.add(outboxEventId)) {
            return;
        }
        Optional<OutboxEvent> maybeEvent = outboxEventRepository.findById(outboxEventId);
        if (maybeEvent.isEmpty() || maybeEvent.get().getPublishedAt() != null) {
            inFlight.remove(outboxEventId);
            return;
        }
        OutboxEvent event = maybeEvent.get();
        try {
            kafkaTemplate.send(event.getTopic(), event.getOrgId().toString(), event.getPayload())
                    .whenComplete((result, ex) -> {
                        inFlight.remove(outboxEventId);
                        if (ex != null) {
                            log.warn("Failed to publish outbox event {}, will retry via the sweep", outboxEventId, ex);
                            return;
                        }
                        event.markPublished();
                        outboxEventRepository.save(event);
                    });
        } catch (Exception ex) {
            inFlight.remove(outboxEventId);
            log.warn("Failed to publish outbox event {}, will retry via the sweep", outboxEventId, ex);
        }
    }
}
