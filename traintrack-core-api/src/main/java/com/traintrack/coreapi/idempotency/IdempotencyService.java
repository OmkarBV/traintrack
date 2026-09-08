package com.traintrack.coreapi.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traintrack.coreapi.domain.IdempotencyKey;
import com.traintrack.coreapi.domain.IdempotencyStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generic idempotency-key guard for POST endpoints, backed by a database
 * table rather than response caching, per the Phase 3 requirement.
 *
 * <p>The table's primary key is the actual concurrency control: {@link #claim}
 * inserts a PENDING row and lets the unique constraint — not application
 * logic — decide which of two simultaneous requests with the same key wins.
 * {@code claim}/{@code complete}/{@code release} all run
 * {@code REQUIRES_NEW} deliberately: once a Postgres transaction hits a
 * constraint violation it can't run any further statements until rolled
 * back, so "did we win the race" has to be resolved in its own transaction,
 * independent of the caller's larger transaction (which is still free to
 * commit or roll back its own business-data changes afterward).
 */
@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyKeyRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public <T> Optional<T> findCompleted(String key, Class<T> responseType) {
        return repository
                .findById(key)
                .filter(record -> record.getStatus() == IdempotencyStatus.COMPLETED)
                .map(record -> deserialize(record.getResponseBody(), responseType));
    }

    /** @return true if this call claimed the key and should proceed with the work. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(String key, UUID orgId, String endpoint) {
        try {
            repository.saveAndFlush(new IdempotencyKey(key, orgId, endpoint));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String key, int responseStatus, Object responseBody) {
        repository.findById(key).ifPresent(record -> record.complete(responseStatus, serialize(responseBody)));
    }

    /** Frees the key after a failed attempt so a retry isn't permanently blocked by it. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(String key) {
        repository.deleteById(key);
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise idempotent response", e);
        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialise idempotent response", e);
        }
    }
}
