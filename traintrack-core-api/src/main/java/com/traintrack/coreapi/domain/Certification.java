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

/** See {@link Enrolment} for why orgId is denormalised here rather than derived via a join. */
@Entity
@Table(name = "certifications")
@Filter(name = TenantFilter.NAME)
public class Certification extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Set once, the first time the expiry-warning job notifies for this certification — see CertificationExpiryService. */
    @Column(name = "expiring_notified_at")
    private Instant expiringNotifiedAt;

    /** S3 object key for the generated PDF certificate; populated in Phase 7, nullable until then. */
    @Column(name = "certificate_url", length = 1024)
    private String certificateUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CertificationStatus status;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Certification() {
    }

    public Certification(
            UUID orgId, User user, Course course, Instant issuedAt, Instant expiresAt, CertificationStatus status) {
        this.orgId = orgId;
        this.user = user;
        this.course = course;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public User getUser() {
        return user;
    }

    public Course getCourse() {
        return course;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getExpiringNotifiedAt() {
        return expiringNotifiedAt;
    }

    public void markExpiringNotified() {
        this.expiringNotifiedAt = Instant.now();
    }

    public String getCertificateUrl() {
        return certificateUrl;
    }

    public void setCertificateUrl(String certificateUrl) {
        this.certificateUrl = certificateUrl;
    }

    public CertificationStatus getStatus() {
        return status;
    }

    public void setStatus(CertificationStatus status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }
}
