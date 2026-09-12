package com.traintrack.coreapi.enrolment;

import com.traintrack.coreapi.audit.AuditPublisher;
import com.traintrack.coreapi.certification.CertificationRepository;
import com.traintrack.coreapi.certification.CertificationMapper;
import com.traintrack.coreapi.certification.dto.CertificationResponse;
import com.traintrack.coreapi.certification.storage.CertificatePdfGenerator;
import com.traintrack.coreapi.certification.storage.CertificateStorageService;
import com.traintrack.coreapi.common.exception.ConflictException;
import com.traintrack.coreapi.common.exception.NotFoundException;
import com.traintrack.coreapi.course.CourseRepository;
import com.traintrack.coreapi.domain.Certification;
import com.traintrack.coreapi.domain.CertificationStatus;
import com.traintrack.coreapi.domain.Course;
import com.traintrack.coreapi.domain.Enrolment;
import com.traintrack.coreapi.domain.EnrolmentStatus;
import com.traintrack.coreapi.domain.User;
import com.traintrack.coreapi.enrolment.dto.EnrolmentCreateRequest;
import com.traintrack.coreapi.enrolment.dto.EnrolmentResponse;
import com.traintrack.coreapi.idempotency.IdempotencyService;
import com.traintrack.coreapi.security.CurrentUser;
import com.traintrack.coreapi.user.UserRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrolmentService {

    private static final String ENROL_ENDPOINT = "POST /api/v1/enrolments";

    private final EnrolmentRepository enrolmentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CertificationRepository certificationRepository;
    private final EnrolmentMapper enrolmentMapper;
    private final CertificationMapper certificationMapper;
    private final IdempotencyService idempotencyService;
    private final AuditPublisher auditPublisher;
    private final CertificatePdfGenerator certificatePdfGenerator;
    private final CertificateStorageService certificateStorageService;

    public EnrolmentService(
            EnrolmentRepository enrolmentRepository,
            UserRepository userRepository,
            CourseRepository courseRepository,
            CertificationRepository certificationRepository,
            EnrolmentMapper enrolmentMapper,
            CertificationMapper certificationMapper,
            IdempotencyService idempotencyService,
            AuditPublisher auditPublisher,
            CertificatePdfGenerator certificatePdfGenerator,
            CertificateStorageService certificateStorageService) {
        this.enrolmentRepository = enrolmentRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.certificationRepository = certificationRepository;
        this.enrolmentMapper = enrolmentMapper;
        this.certificationMapper = certificationMapper;
        this.idempotencyService = idempotencyService;
        this.auditPublisher = auditPublisher;
        this.certificatePdfGenerator = certificatePdfGenerator;
        this.certificateStorageService = certificateStorageService;
    }

    /**
     * Only successful (201) outcomes are recorded for replay — a failed
     * attempt (bad request, not-found user/course) rolls back entirely and
     * the idempotency key is released, so a corrected retry is processed
     * fresh rather than replaying the original failure.
     */
    @Transactional
    public EnrolmentResponse createIdempotent(String idempotencyKey, EnrolmentCreateRequest request) {
        var existing = idempotencyService.findCompleted(idempotencyKey, EnrolmentResponse.class);
        if (existing.isPresent()) {
            return existing.get();
        }

        UUID orgId = CurrentUser.requireOrgId();
        boolean claimed = idempotencyService.claim(idempotencyKey, orgId, ENROL_ENDPOINT);
        if (!claimed) {
            return idempotencyService
                    .findCompleted(idempotencyKey, EnrolmentResponse.class)
                    .orElseThrow(() -> new ConflictException(
                            "A request with idempotency key '" + idempotencyKey + "' is already being processed"));
        }

        try {
            EnrolmentResponse response = enrolmentMapper.toResponse(doCreate(request, orgId));
            idempotencyService.complete(idempotencyKey, HttpStatus.CREATED.value(), response);
            return response;
        } catch (RuntimeException e) {
            idempotencyService.release(idempotencyKey);
            throw e;
        }
    }

    private Enrolment doCreate(EnrolmentCreateRequest request, UUID orgId) {
        User user = userRepository
                .findByIdScoped(request.userId())
                .orElseThrow(() -> new NotFoundException("User not found: " + request.userId()));
        Course course = courseRepository
                .findByIdScoped(request.courseId())
                .orElseThrow(() -> new NotFoundException("Course not found: " + request.courseId()));
        Enrolment saved =
                enrolmentRepository.save(new Enrolment(orgId, course, user, EnrolmentStatus.ENROLLED, Instant.now()));
        auditPublisher.record(
                orgId,
                CurrentUser.requireUserId(),
                "Enrolment",
                saved.getId(),
                "ENROLMENT_CREATED",
                "User '" + user.getEmail() + "' enrolled in course '" + course.getTitle() + "'");
        return saved;
    }

    /**
     * Returns the newly issued certification (the resource this action
     * actually creates), not the enrolment — a PATCH that only echoed the
     * enrolment's now-COMPLETED status back would hide the interesting part
     * of the response.
     */
    @Transactional
    public CertificationResponse complete(UUID enrolmentId) {
        Enrolment enrolment = enrolmentRepository
                .findByIdScoped(enrolmentId)
                .orElseThrow(() -> new NotFoundException("Enrolment not found: " + enrolmentId));

        if (enrolment.getStatus() == EnrolmentStatus.COMPLETED) {
            throw new ConflictException("Enrolment is already completed");
        }
        if (enrolment.getStatus() == EnrolmentStatus.FAILED) {
            throw new ConflictException("Cannot complete a failed enrolment");
        }

        Instant now = Instant.now();
        enrolment.setStatus(EnrolmentStatus.COMPLETED);
        enrolment.setCompletedAt(now);

        Course course = enrolment.getCourse();
        // Calendar-aware month arithmetic (handles month-length differences,
        // e.g. Jan 31 + 1 month -> Feb 28) — Instant alone can't do this.
        Instant expiresAt =
                ZonedDateTime.ofInstant(now, ZoneOffset.UTC).plusMonths(course.getValidityMonths()).toInstant();

        Certification certification = new Certification(
                enrolment.getOrgId(), enrolment.getUser(), course, now, expiresAt, CertificationStatus.ACTIVE);
        Certification savedCertification = certificationRepository.save(certification);

        // Generated and uploaded synchronously, in the same transaction as
        // the certification row: a certification without a retrievable
        // certificate document is a bug, not a valid intermediate state, so
        // if S3 is unreachable the whole completion should roll back rather
        // than leave the row half-finished. The trade-off — a DB transaction
        // held open for the duration of an S3 call — is deliberate for this
        // scale; see the README for what changes at higher throughput.
        byte[] pdfBytes = certificatePdfGenerator.generate(savedCertification);
        String certificateKey =
                certificateStorageService.upload(enrolment.getOrgId(), savedCertification.getId(), pdfBytes);
        savedCertification.setCertificateUrl(certificateKey);

        UUID actorId = CurrentUser.requireUserId();
        auditPublisher.record(
                enrolment.getOrgId(),
                actorId,
                "Enrolment",
                enrolment.getId(),
                "ENROLMENT_COMPLETED",
                "Enrolment completed for course '" + course.getTitle() + "'");
        auditPublisher.record(
                enrolment.getOrgId(),
                actorId,
                "Certification",
                savedCertification.getId(),
                "CERTIFICATION_ISSUED",
                "Certification issued for course '" + course.getTitle() + "', expires " + expiresAt);

        return certificationMapper.toResponse(savedCertification);
    }
}
