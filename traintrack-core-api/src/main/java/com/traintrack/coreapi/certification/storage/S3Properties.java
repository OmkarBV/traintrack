package com.traintrack.coreapi.certification.storage;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "traintrack.s3")
public record S3Properties(
        String bucket, String region, String endpoint, String accessKey, String secretKey, Duration presignedUrlTtl) {
}
