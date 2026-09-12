package com.traintrack.coreapi.certification;

import com.traintrack.coreapi.certification.dto.CertificateDownloadResponse;
import com.traintrack.coreapi.certification.dto.CertificationResponse;
import com.traintrack.coreapi.domain.CertificationStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CertificationController {

    private final CertificationService certificationService;
    private final CertificationMapper certificationMapper;

    public CertificationController(CertificationService certificationService, CertificationMapper certificationMapper) {
        this.certificationService = certificationService;
        this.certificationMapper = certificationMapper;
    }

    @GetMapping("/api/v1/certifications")
    @PreAuthorize("hasAuthority('CERT_VIEW_ALL')")
    public Page<CertificationResponse> search(
            @RequestParam(required = false) CertificationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant expiringBefore,
            @PageableDefault(size = 20) Pageable pageable) {
        return certificationService.search(status, expiringBefore, pageable).map(certificationMapper::toResponse);
    }

    /**
     * A caller with only CERT_VIEW_OWN (no CERT_VIEW_ALL) may only request
     * their own user id — enforced here via SpEL rather than in the service,
     * since it's purely an authorisation rule, not business logic.
     */
    @GetMapping("/api/v1/users/{id}/certifications")
    @PreAuthorize("hasAuthority('CERT_VIEW_ALL') or (hasAuthority('CERT_VIEW_OWN') and #id == authentication.principal.userId())")
    public Page<CertificationResponse> byUser(@PathVariable UUID id, @PageableDefault(size = 20) Pageable pageable) {
        return certificationService.findByUser(id, pageable).map(certificationMapper::toResponse);
    }

    /**
     * Coarse-grained gate here (has either permission at all); the
     * fine-grained "is this actually the caller's own certificate" check
     * happens in the service, against the certification that id resolves to
     * — see {@link CertificationService#generateDownloadUrl}.
     */
    @GetMapping("/api/v1/certifications/{id}/download-url")
    @PreAuthorize("hasAuthority('CERT_VIEW_ALL') or hasAuthority('CERT_VIEW_OWN')")
    public CertificateDownloadResponse downloadUrl(@PathVariable UUID id) {
        return certificationService.generateDownloadUrl(id);
    }
}
