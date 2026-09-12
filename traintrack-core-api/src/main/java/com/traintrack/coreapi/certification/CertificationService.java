package com.traintrack.coreapi.certification;

import com.traintrack.coreapi.certification.dto.CertificateDownloadResponse;
import com.traintrack.coreapi.certification.storage.CertificateStorageService;
import com.traintrack.coreapi.common.exception.NotFoundException;
import com.traintrack.coreapi.domain.Certification;
import com.traintrack.coreapi.domain.CertificationStatus;
import com.traintrack.coreapi.security.AuthenticatedUser;
import com.traintrack.coreapi.security.CurrentUser;
import java.net.URL;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CertificationService {

    private final CertificationRepository certificationRepository;
    private final CertificateStorageService certificateStorageService;

    public CertificationService(
            CertificationRepository certificationRepository, CertificateStorageService certificateStorageService) {
        this.certificationRepository = certificationRepository;
        this.certificateStorageService = certificateStorageService;
    }

    @Transactional(readOnly = true)
    public Page<Certification> search(CertificationStatus status, Instant expiringBefore, Pageable pageable) {
        Specification<Certification> spec = Specification.where(CertificationSpecifications.hasStatus(status))
                .and(CertificationSpecifications.expiresBefore(expiringBefore));
        return certificationRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Certification> findByUser(UUID userId, Pageable pageable) {
        return certificationRepository.findByUserId(userId, pageable);
    }

    /**
     * A caller with only CERT_VIEW_OWN can list their own certifications by
     * user id (enforced by the controller's SpEL check), but downloading is
     * addressed by certification id — there's no path variable to compare
     * against the caller's id, so the ownership check has to happen here,
     * against the certification actually fetched.
     */
    @Transactional(readOnly = true)
    public CertificateDownloadResponse generateDownloadUrl(UUID certificationId) {
        Certification certification = certificationRepository
                .findByIdScoped(certificationId)
                .orElseThrow(() -> new NotFoundException("Certification not found: " + certificationId));

        AuthenticatedUser caller = CurrentUser.get()
                .orElseThrow(() -> new IllegalStateException("No authenticated user in context"));
        boolean canViewAny = caller.permissions().contains("CERT_VIEW_ALL");
        if (!canViewAny && !certification.getUser().getId().equals(caller.userId())) {
            throw new AccessDeniedException("You do not have permission to download this certificate");
        }

        String key = certification.getCertificateUrl();
        if (key == null) {
            throw new NotFoundException("No certificate document is available for certification: " + certificationId);
        }

        URL url = certificateStorageService.presignDownloadUrl(key);
        Instant expiresAt = Instant.now().plus(certificateStorageService.presignedUrlTtl());
        return new CertificateDownloadResponse(url.toString(), expiresAt);
    }
}
