package com.traintrack.coreapi.certification.expiry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.traintrack.coreapi.audit.AuditPublisher;
import com.traintrack.coreapi.certification.CertificationRepository;
import com.traintrack.coreapi.domain.Certification;
import com.traintrack.coreapi.domain.CertificationStatus;
import com.traintrack.coreapi.domain.Course;
import com.traintrack.coreapi.domain.CourseStatus;
import com.traintrack.coreapi.domain.Organisation;
import com.traintrack.coreapi.domain.User;
import com.traintrack.coreapi.domain.UserStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CertificationExpiryServiceTest {

    @Mock
    private CertificationRepository certificationRepository;

    @Mock
    private CertificationExpiryEventPublisher eventPublisher;

    @Mock
    private AuditPublisher auditPublisher;

    private CertificationExpiryService service;

    @BeforeEach
    void setUp() {
        service = new CertificationExpiryService(certificationRepository, eventPublisher, auditPublisher);
    }

    @Test
    void markLapsedAsExpiredTransitionsEachMatchingCertificationAndAudits() {
        Certification cert = certification();
        when(certificationRepository.findLapsed(eq(CertificationStatus.ACTIVE), any(Instant.class))).thenReturn(List.of(cert));

        int count = service.markLapsedAsExpired();

        assertThat(count).isEqualTo(1);
        assertThat(cert.getStatus()).isEqualTo(CertificationStatus.EXPIRED);
        verify(auditPublisher)
                .record(eq(cert.getOrgId()), isNull(), eq("Certification"), eq(cert.getId()), eq("CERTIFICATION_EXPIRED"), any());
    }

    @Test
    void markLapsedAsExpiredIsANoOpWhenNothingIsLapsed() {
        when(certificationRepository.findLapsed(eq(CertificationStatus.ACTIVE), any(Instant.class))).thenReturn(List.of());

        int count = service.markLapsedAsExpired();

        assertThat(count).isZero();
        verify(auditPublisher, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void publishExpiringSoonNotificationsPublishesAndMarksEachCertificationNotified() {
        Certification cert = certification();
        when(certificationRepository.findExpiringSoonUnnotified(eq(CertificationStatus.ACTIVE), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(cert));

        int count = service.publishExpiringSoonNotifications();

        assertThat(count).isEqualTo(1);
        assertThat(cert.getExpiringNotifiedAt()).isNotNull();
        verify(eventPublisher).publishExpiringSoon(cert);
    }

    @Test
    void publishExpiringSoonNotificationsIsANoOpWhenNothingQualifies() {
        when(certificationRepository.findExpiringSoonUnnotified(eq(CertificationStatus.ACTIVE), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        int count = service.publishExpiringSoonNotifications();

        assertThat(count).isZero();
        verify(eventPublisher, never()).publishExpiringSoon(any());
    }

    private Certification certification() {
        Organisation org = new Organisation("Acme");
        User user = new User(org, "e@acme.test", "hash", "Emp", UserStatus.ACTIVE);
        Course course = new Course(org, "Fire Safety", "d", 4, 12, CourseStatus.PUBLISHED);
        return new Certification(
                UUID.randomUUID(), user, course, Instant.now(), Instant.now().plus(10, ChronoUnit.DAYS), CertificationStatus.ACTIVE);
    }
}
