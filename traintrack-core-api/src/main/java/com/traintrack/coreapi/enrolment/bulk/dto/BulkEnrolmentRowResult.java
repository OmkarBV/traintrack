package com.traintrack.coreapi.enrolment.bulk.dto;

import com.traintrack.coreapi.domain.BulkJobRowStatus;
import java.util.UUID;

public record BulkEnrolmentRowResult(
        int rowNumber, String userId, String courseId, BulkJobRowStatus status, UUID enrolmentId, String errorMessage) {
}
