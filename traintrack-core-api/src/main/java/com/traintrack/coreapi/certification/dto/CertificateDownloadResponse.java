package com.traintrack.coreapi.certification.dto;

import java.time.Instant;

public record CertificateDownloadResponse(String url, Instant expiresAt) {
}
