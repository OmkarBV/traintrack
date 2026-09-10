package com.traintrack.coreapi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Not org-filtered directly (no org_id column) — access is always via a job
 * fetched through {@code BulkEnrolmentJobRepository.findByIdScoped} first,
 * which is filtered, so an unfiltered row lookup by an already-authorised
 * job id can't leak across tenants.
 */
@Entity
@Table(name = "bulk_enrolment_job_rows")
public class BulkEnrolmentJobRow {

    @EmbeddedId
    private BulkEnrolmentJobRowId id;

    @Column(name = "raw_user_id", nullable = false)
    private String rawUserId;

    @Column(name = "raw_course_id", nullable = false)
    private String rawCourseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BulkJobRowStatus status;

    @Column(name = "enrolment_id")
    private UUID enrolmentId;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    protected BulkEnrolmentJobRow() {
    }

    public static BulkEnrolmentJobRow success(
            UUID jobId, int rowNumber, String rawUserId, String rawCourseId, UUID enrolmentId) {
        BulkEnrolmentJobRow row = new BulkEnrolmentJobRow();
        row.id = new BulkEnrolmentJobRowId(jobId, rowNumber);
        row.rawUserId = rawUserId;
        row.rawCourseId = rawCourseId;
        row.status = BulkJobRowStatus.SUCCESS;
        row.enrolmentId = enrolmentId;
        return row;
    }

    public static BulkEnrolmentJobRow failure(UUID jobId, int rowNumber, String rawUserId, String rawCourseId, String errorMessage) {
        BulkEnrolmentJobRow row = new BulkEnrolmentJobRow();
        row.id = new BulkEnrolmentJobRowId(jobId, rowNumber);
        row.rawUserId = rawUserId;
        row.rawCourseId = rawCourseId;
        row.status = BulkJobRowStatus.FAILED;
        row.errorMessage = errorMessage;
        return row;
    }

    public BulkEnrolmentJobRowId getId() {
        return id;
    }

    public String getRawUserId() {
        return rawUserId;
    }

    public String getRawCourseId() {
        return rawCourseId;
    }

    public BulkJobRowStatus getStatus() {
        return status;
    }

    public UUID getEnrolmentId() {
        return enrolmentId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
