package com.traintrack.coreapi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

/**
 * Not org-filtered like the other entities: rows are always looked up by the
 * exact client-supplied key, never listed or browsed, so there's no
 * cross-tenant enumeration surface to guard against here.
 *
 * <p>Implements {@link Persistable} with {@code isNew()} always true so
 * {@code IdempotencyService.claim()}'s {@code saveAndFlush()} always attempts
 * a real INSERT. Without this, Spring Data JPA's default "is this new" check
 * for a manually-assigned (non-{@code @GeneratedValue}) id routes every save
 * through {@code merge()} — which, for a key that already has a committed row
 * (e.g. left PENDING by a request that crashed between claim and complete),
 * would silently update that row and report success instead of the
 * constraint violation {@code claim()} depends on to detect "already
 * claimed".
 */
@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKey implements Persistable<String> {

    @Id
    private String id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(nullable = false)
    private String endpoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IdempotencyStatus status;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdempotencyKey() {
    }

    public IdempotencyKey(String id, UUID orgId, String endpoint) {
        this.id = id;
        this.orgId = orgId;
        this.endpoint = endpoint;
        this.status = IdempotencyStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void complete(int responseStatus, String responseBody) {
        this.status = IdempotencyStatus.COMPLETED;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return true;
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
