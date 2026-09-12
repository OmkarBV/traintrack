package com.traintrack.coreapi.certification.expiry;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Thin cron + distributed-lock wrapper — see {@link CertificationExpiryService}
 * for the actual logic, and {@code ShedLockConfig} for why the lock matters.
 * {@code lockAtLeastFor} guards against a job that finishes very quickly on a
 * clock-skewed instance re-triggering immediately on another; it does not
 * need to be long, just longer than any plausible clock drift.
 */
@Component
public class CertificationExpiryScheduler {

    private final CertificationExpiryService certificationExpiryService;

    public CertificationExpiryScheduler(CertificationExpiryService certificationExpiryService) {
        this.certificationExpiryService = certificationExpiryService;
    }

    @Scheduled(cron = "${traintrack.jobs.mark-lapsed-certifications-cron:0 0 1 * * *}")
    @SchedulerLock(name = "markLapsedCertificationsExpired", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    public void markLapsedCertificationsExpired() {
        certificationExpiryService.markLapsedAsExpired();
    }

    @Scheduled(cron = "${traintrack.jobs.publish-expiring-certifications-cron:0 0 2 * * *}")
    @SchedulerLock(name = "publishExpiringCertificationEvents", lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    public void publishExpiringCertificationEvents() {
        certificationExpiryService.publishExpiringSoonNotifications();
    }
}
