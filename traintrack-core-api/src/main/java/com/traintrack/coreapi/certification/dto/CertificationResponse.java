package com.traintrack.coreapi.certification.dto;

import com.traintrack.coreapi.domain.CertificationStatus;
import java.time.Instant;
import java.util.UUID;

public record CertificationResponse(
        UUID id,
        UUID userId,
        UUID courseId,
        Instant issuedAt,
        Instant expiresAt,
        CertificationStatus status,
        String certificateUrl) {
}
