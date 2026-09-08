package com.traintrack.coreapi.certification;

import com.traintrack.coreapi.domain.Certification;
import com.traintrack.coreapi.domain.CertificationStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CertificationService {

    private final CertificationRepository certificationRepository;

    public CertificationService(CertificationRepository certificationRepository) {
        this.certificationRepository = certificationRepository;
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
}
