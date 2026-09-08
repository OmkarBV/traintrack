package com.traintrack.coreapi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Filter;

/**
 * orgId is denormalised onto this entity (rather than derived via a join to
 * course/user) so that the tenant-isolation mechanism introduced in Phase 2 —
 * a Hibernate {@code @Filter} — can filter on a plain scalar column present on
 * every tenant-scoped entity, without needing a join at query time.
 */
@Entity
@Table(name = "enrolments")
@Filter(name = TenantFilter.NAME)
public class Enrolment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrolmentStatus status;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Enrolment() {
    }

    public Enrolment(UUID orgId, Course course, User user, EnrolmentStatus status, Instant enrolledAt) {
        this.orgId = orgId;
        this.course = course;
        this.user = user;
        this.status = status;
        this.enrolledAt = enrolledAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public Course getCourse() {
        return course;
    }

    public User getUser() {
        return user;
    }

    public EnrolmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnrolmentStatus status) {
        this.status = status;
    }

    public Instant getEnrolledAt() {
        return enrolledAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Long getVersion() {
        return version;
    }
}
