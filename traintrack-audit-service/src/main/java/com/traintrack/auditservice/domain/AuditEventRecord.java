package com.traintrack.auditservice.domain;

import com.traintrack.common.event.AuditEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

/**
 * The persisted form of a consumed {@link AuditEvent}. Named distinctly
 * (not just "AuditEvent") so both types can be imported in the same file —
 * e.g. the constructor below — without ambiguity.
 *
 * <p>Implements {@link Persistable} with {@code isNew()} always true so
 * {@code repository.save()} always goes through {@code EntityManager.persist()}
 * (a true INSERT), never {@code merge()}. Without this, Spring Data JPA's
 * default "is this new" check for an entity with a manually-assigned
 * (non-{@code @GeneratedValue}) id treats every save as an update-if-exists,
 * which would silently upsert a duplicate delivery instead of hitting the
 * unique constraint on {@code event_id} the way AuditEventConsumer expects.
 */
@Entity
@Table(name = "audit_events")
public class AuditEventRecord implements Persistable<UUID> {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "source_application", nullable = false)
    private String sourceApplication;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(nullable = false)
    private String action;

    @Column(name = "change_summary", columnDefinition = "text")
    private String changeSummary;

    protected AuditEventRecord() {
    }

    public AuditEventRecord(AuditEvent event) {
        this.eventId = event.eventId();
        this.occurredAt = event.occurredAt();
        this.receivedAt = Instant.now();
        this.sourceApplication = event.sourceApplication();
        this.orgId = event.orgId();
        this.actorUserId = event.actorUserId();
        this.entityType = event.entityType();
        this.entityId = event.entityId();
        this.action = event.action();
        this.changeSummary = event.changeSummary();
    }

    @Override
    public UUID getId() {
        return eventId;
    }

    @Override
    public boolean isNew() {
        // Audit events are write-once and immutable; every save() is an
        // insert attempt, never an update.
        return true;
    }

    public UUID getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public String getSourceApplication() {
        return sourceApplication;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getAction() {
        return action;
    }

    public String getChangeSummary() {
        return changeSummary;
    }
}
