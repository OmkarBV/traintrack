package com.traintrack.coreapi.certification.storage;

import java.net.URL;
import java.time.Duration;
import java.util.UUID;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * Certificates are stored under a key namespaced by org and certification id
 * ({@code certificates/<orgId>/<certificationId>.pdf}) — not because the
 * bucket itself is shared across tenants in any way callers can exploit (a
 * key is only ever reachable via {@link #presignDownloadUrl}, gated by the
 * same tenant/ownership checks as every other certification read), but so
 * that a bucket listing or a support engineer poking around S3 directly
 * still reads as organised per-org data rather than one flat, unscoped pile.
 */
@Service
public class CertificateStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    public CertificateStorageService(S3Client s3Client, S3Presigner s3Presigner, S3Properties properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    public String upload(UUID orgId, UUID certificationId, byte[] pdfBytes) {
        String key = key(orgId, certificationId);
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(properties.bucket())
                        .key(key)
                        .contentType("application/pdf")
                        .build(),
                RequestBody.fromBytes(pdfBytes));
        return key;
    }

    public URL presignDownloadUrl(String key) {
        GetObjectRequest getObjectRequest =
                GetObjectRequest.builder().bucket(properties.bucket()).key(key).build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(properties.presignedUrlTtl())
                .getObjectRequest(getObjectRequest)
                .build();
        return s3Presigner.presignGetObject(presignRequest).url();
    }

    public Duration presignedUrlTtl() {
        return properties.presignedUrlTtl();
    }

    private String key(UUID orgId, UUID certificationId) {
        return "certificates/%s/%s.pdf".formatted(orgId, certificationId);
    }
}
