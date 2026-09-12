package com.traintrack.coreapi.enrolment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.mockito.ArgumentMatchers.eq;

import com.traintrack.coreapi.audit.AuditPublisher;
import com.traintrack.coreapi.certification.CertificationMapper;
import com.traintrack.coreapi.certification.CertificationRepository;
import com.traintrack.coreapi.certification.dto.CertificationResponse;
import com.traintrack.coreapi.certification.storage.CertificatePdfGenerator;
import com.traintrack.coreapi.certification.storage.CertificateStorageService;
import com.traintrack.coreapi.common.exception.ConflictException;
import com.traintrack.coreapi.common.exception.NotFoundException;
import com.traintrack.coreapi.course.CourseRepository;
import com.traintrack.coreapi.domain.Certification;
import com.traintrack.coreapi.domain.CertificationStatus;
import com.traintrack.coreapi.domain.Course;
import com.traintrack.coreapi.domain.CourseStatus;
import com.traintrack.coreapi.domain.Enrolment;
import com.traintrack.coreapi.domain.EnrolmentStatus;
import com.traintrack.coreapi.domain.Organisation;
import com.traintrack.coreapi.domain.User;
import com.traintrack.coreapi.domain.UserStatus;
import com.traintrack.coreapi.enrolment.dto.EnrolmentCreateRequest;
import com.traintrack.coreapi.enrolment.dto.EnrolmentResponse;
import com.traintrack.coreapi.idempotency.IdempotencyService;
import com.traintrack.coreapi.security.AuthenticatedUser;
import com.traintrack.coreapi.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class EnrolmentServiceTest {

    @Mock
    private EnrolmentRepository enrolmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CertificationRepository certificationRepository;

    @Mock
    private EnrolmentMapper enrolmentMapper;

    @Mock
    private CertificationMapper certificationMapper;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private AuditPublisher auditPublisher;

    @Mock
    private CertificatePdfGenerator certificatePdfGenerator;

    @Mock
    private CertificateStorageService certificateStorageService;

    private EnrolmentService enrolmentService;
    private final UUID orgId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        enrolmentService = new EnrolmentService(
                enrolmentRepository,
                userRepository,
                courseRepository,
                certificationRepository,
                enrolmentMapper,
                certificationMapper,
                idempotencyService,
                auditPublisher,
                certificatePdfGenerator,
                certificateStorageService);
        AuthenticatedUser principal =
                new AuthenticatedUser(UUID.randomUUID(), orgId, "admin@acme.test", Set.of("ENROLMENT_CREATE"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createIdempotentReplaysAnAlreadyCompletedRequest() {
        EnrolmentResponse cached = new EnrolmentResponse(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), EnrolmentStatus.ENROLLED, Instant.now(), null);
        when(idempotencyService.findCompleted("key-1", EnrolmentResponse.class)).thenReturn(Optional.of(cached));

        EnrolmentResponse result = enrolmentService.createIdempotent(
                "key-1", new EnrolmentCreateRequest(UUID.randomUUID(), UUID.randomUUID()));

        assertThat(result).isSameAs(cached);
        verify(enrolmentRepository, never()).save(any());
    }

    @Test
    void createIdempotentPersistsAndCompletesOnFirstAttempt() {
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        User user = new User(new Organisation("Acme"), "e@acme.test", "hash", "Emp", UserStatus.ACTIVE);
        Course course = new Course(new Organisation("Acme"), "Fire Safety", "d", 4, 12, CourseStatus.PUBLISHED);
        Enrolment saved = new Enrolment(orgId, course, user, EnrolmentStatus.ENROLLED, Instant.now());
        EnrolmentResponse response =
                new EnrolmentResponse(UUID.randomUUID(), userId, courseId, EnrolmentStatus.ENROLLED, Instant.now(), null);

        when(idempotencyService.findCompleted("key-2", EnrolmentResponse.class)).thenReturn(Optional.empty());
        when(idempotencyService.claim("key-2", orgId, "POST /api/v1/enrolments")).thenReturn(true);
        when(userRepository.findByIdScoped(userId)).thenReturn(Optional.of(user));
        when(courseRepository.findByIdScoped(courseId)).thenReturn(Optional.of(course));
        when(enrolmentRepository.save(any(Enrolment.class))).thenReturn(saved);
        when(enrolmentMapper.toResponse(saved)).thenReturn(response);

        EnrolmentResponse result = enrolmentService.createIdempotent("key-2", new EnrolmentCreateRequest(userId, courseId));

        assertThat(result).isEqualTo(response);
        verify(idempotencyService).complete("key-2", 201, response);
    }

    @Test
    void createIdempotentReturnsConflictWhenClaimLostAndNothingCompletedYet() {
        when(idempotencyService.findCompleted("key-3", EnrolmentResponse.class)).thenReturn(Optional.empty());
        when(idempotencyService.claim("key-3", orgId, "POST /api/v1/enrolments")).thenReturn(false);

        assertThatThrownBy(() -> enrolmentService.createIdempotent(
                        "key-3", new EnrolmentCreateRequest(UUID.randomUUID(), UUID.randomUUID())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createIdempotentReleasesTheKeyWhenCreationFails() {
        UUID userId = UUID.randomUUID();
        when(idempotencyService.findCompleted("key-4", EnrolmentResponse.class)).thenReturn(Optional.empty());
        when(idempotencyService.claim("key-4", orgId, "POST /api/v1/enrolments")).thenReturn(true);
        when(userRepository.findByIdScoped(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrolmentService.createIdempotent(
                        "key-4", new EnrolmentCreateRequest(userId, UUID.randomUUID())))
                .isInstanceOf(NotFoundException.class);

        verify(idempotencyService).release("key-4");
        verify(idempotencyService, never()).complete(any(), anyInt(), any());
    }

    @Test
    void completeThrowsNotFoundWhenEnrolmentMissing() {
        UUID id = UUID.randomUUID();
        when(enrolmentRepository.findByIdScoped(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrolmentService.complete(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void completeRejectsAnAlreadyCompletedEnrolment() {
        Enrolment enrolment = enrolmentOf(EnrolmentStatus.COMPLETED);
        UUID id = UUID.randomUUID();
        when(enrolmentRepository.findByIdScoped(id)).thenReturn(Optional.of(enrolment));

        assertThatThrownBy(() -> enrolmentService.complete(id)).isInstanceOf(ConflictException.class);
    }

    @Test
    void completeRejectsAFailedEnrolment() {
        Enrolment enrolment = enrolmentOf(EnrolmentStatus.FAILED);
        UUID id = UUID.randomUUID();
        when(enrolmentRepository.findByIdScoped(id)).thenReturn(Optional.of(enrolment));

        assertThatThrownBy(() -> enrolmentService.complete(id)).isInstanceOf(ConflictException.class);
    }

    @Test
    void completeIssuesACertificationExpiringAfterCourseValidityMonths() {
        Enrolment enrolment = enrolmentOf(EnrolmentStatus.ENROLLED);
        UUID id = UUID.randomUUID();
        byte[] pdfBytes = "pdf-bytes".getBytes();
        when(enrolmentRepository.findByIdScoped(id)).thenReturn(Optional.of(enrolment));
        when(certificationRepository.save(any(Certification.class))).thenAnswer(inv -> inv.getArgument(0));
        when(certificatePdfGenerator.generate(any(Certification.class))).thenReturn(pdfBytes);
        when(certificateStorageService.upload(eq(orgId), any(), eq(pdfBytes))).thenReturn("certificates/key.pdf");
        when(certificationMapper.toResponse(any(Certification.class))).thenAnswer(inv -> {
            Certification c = inv.getArgument(0);
            return new CertificationResponse(
                    UUID.randomUUID(), null, null, c.getIssuedAt(), c.getExpiresAt(), c.getStatus(), c.getCertificateUrl());
        });

        CertificationResponse response = enrolmentService.complete(id);

        assertThat(enrolment.getStatus()).isEqualTo(EnrolmentStatus.COMPLETED);
        assertThat(enrolment.getCompletedAt()).isNotNull();
        assertThat(response.status()).isEqualTo(CertificationStatus.ACTIVE);
        assertThat(response.expiresAt()).isAfter(response.issuedAt());
        assertThat(response.certificateUrl()).isEqualTo("certificates/key.pdf");
    }

    private Enrolment enrolmentOf(EnrolmentStatus status) {
        Course course = new Course(new Organisation("Acme"), "Fire Safety", "d", 4, 6, CourseStatus.PUBLISHED);
        User user = new User(new Organisation("Acme"), "e@acme.test", "hash", "Emp", UserStatus.ACTIVE);
        return new Enrolment(orgId, course, user, status, Instant.now());
    }
}
