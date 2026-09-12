package com.traintrack.coreapi.certification.expiry;

import com.traintrack.coreapi.audit.AuditPublisher;
import com.traintrack.coreapi.certification.CertificationRepository;
import com.traintrack.coreapi.domain.Certification;
import com.traintrack.coreapi.domain.CertificationStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for the two daily certification-expiry jobs, kept separate
 * from {@link CertificationExpiryScheduler} (the thin @Scheduled/@SchedulerLock
 * wrapper) so it's plain, Mockito-testable business logic with no Spring
 * scheduling infrastructure involved.
 */
@Service
public class CertificationExpiryService {

    private static final Logger log = LoggerFactory.getLogger(CertificationExpiryService.class);
    private static final int EXPIRY_WARNING_WINDOW_DAYS = 30;

    private final CertificationRepository certificationRepository;
    private final CertificationExpiryEventPublisher eventPublisher;
    private final AuditPublisher auditPublisher;

    public CertificationExpiryService(
            CertificationRepository certificationRepository,
            CertificationExpiryEventPublisher eventPublisher,
            AuditPublisher auditPublisher) {
        this.certificationRepository = certificationRepository;
        this.eventPublisher = eventPublisher;
        this.auditPublisher = auditPublisher;
    }

    /**
     * Naturally idempotent: only ever touches certifications still ACTIVE
     * and already past their expiry date, so re-running it (accidentally
     * twice in a row, or simply the next day) finds nothing left to do for
     * any certification it already transitioned.
     */
    @Transactional
    public int markLapsedAsExpired() {
        List<Certification> lapsed = certificationRepository.findLapsed(CertificationStatus.ACTIVE, Instant.now());
        for (Certification certification : lapsed) {
            certification.setStatus(CertificationStatus.EXPIRED);
            auditPublisher.record(
                    certification.getOrgId(),
                    null,
                    "Certification",
                    certification.getId(),
                    "CERTIFICATION_EXPIRED",
                    "Certification for course '" + certification.getCourse().getTitle() + "' expired");
        }
        log.info("Marked {} lapsed certification(s) as EXPIRED", lapsed.size());
        return lapsed.size();
    }

    /**
     * Idempotent by design, not just by accident: {@code expiringNotifiedAt}
     * is set the moment a certification is published, and the query only
     * ever selects rows where it's still null — so a certification is
     * notified about exactly once, the first time it enters the 30-day
     * window, not once per day for the remainder of that window.
     */
    @Transactional
    public int publishExpiringSoonNotifications() {
        Instant now = Instant.now();
        Instant windowEnd = now.plus(EXPIRY_WARNING_WINDOW_DAYS, ChronoUnit.DAYS);
        List<Certification> expiringSoon =
                certificationRepository.findExpiringSoonUnnotified(CertificationStatus.ACTIVE, now, windowEnd);
        for (Certification certification : expiringSoon) {
            eventPublisher.publishExpiringSoon(certification);
            certification.markExpiringNotified();
        }
        log.info("Published {} certification.expiring event(s)", expiringSoon.size());
        return expiringSoon.size();
    }
}
