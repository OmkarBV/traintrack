package com.traintrack.coreapi.enrolment.dto;

import com.traintrack.coreapi.domain.EnrolmentStatus;
import java.time.Instant;
import java.util.UUID;

public record EnrolmentResponse(
        UUID id, UUID userId, UUID courseId, EnrolmentStatus status, Instant enrolledAt, Instant completedAt) {
}
