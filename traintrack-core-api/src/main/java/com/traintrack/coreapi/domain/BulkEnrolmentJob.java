package com.traintrack.coreapi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Filter;

/**
 * Unlike OutboxEvent/IdempotencyKey/AuditEventRecord, this entity is
 * genuinely mutable — the whole point of {@link #markProcessing()} and
 * {@link #markCompleted()} is to update the same row multiple times — so it
 * deliberately does NOT implement {@code Persistable} the way those do.
 * Spring Data JPA's default merge()-on-save for a manually-assigned id is
 * exactly the right behaviour here, not a bug to work around.
 */
@Entity
@Table(name = "bulk_enrolment_jobs")
@Filter(name = TenantFilter.NAME)
public class BulkEnrolmentJob {

    @Id
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BulkJobStatus status;

    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    protected BulkEnrolmentJob() {
    }

    public BulkEnrolmentJob(UUID orgId, int totalRows, UUID createdBy) {
        this.id = UUID.randomUUID();
        this.orgId = orgId;
        this.status = BulkJobStatus.PENDING;
        this.totalRows = totalRows;
        this.createdAt = Instant.now();
        this.createdBy = createdBy;
    }

    public void markProcessing() {
        this.status = BulkJobStatus.PROCESSING;
    }

    public void markCompleted() {
        this.status = BulkJobStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public BulkJobStatus getStatus() {
        return status;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }
}
