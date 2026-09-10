package com.traintrack.coreapi.enrolment.bulk.dto;

import com.traintrack.coreapi.domain.BulkJobStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BulkEnrolmentJobResponse(
        UUID jobId,
        BulkJobStatus status,
        int totalRows,
        long succeededRows,
        long failedRows,
        Instant createdAt,
        Instant completedAt,
        List<BulkEnrolmentRowResult> rows) {
}
