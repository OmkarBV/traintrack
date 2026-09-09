package com.traintrack.coreapi.audit;

import java.util.UUID;

/** Internal signal only — published within the business transaction, consumed after it commits. */
public record OutboxEventReady(UUID outboxEventId) {
}
