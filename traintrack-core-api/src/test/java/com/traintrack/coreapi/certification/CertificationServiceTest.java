package com.traintrack.coreapi.certification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.traintrack.coreapi.certification.dto.CertificateDownloadResponse;
import com.traintrack.coreapi.certification.storage.CertificateStorageService;
import com.traintrack.coreapi.common.exception.NotFoundException;
import com.traintrack.coreapi.domain.Certification;
import com.traintrack.coreapi.domain.CertificationStatus;
import com.traintrack.coreapi.domain.Course;
import com.traintrack.coreapi.domain.CourseStatus;
import com.traintrack.coreapi.domain.Organisation;
import com.traintrack.coreapi.domain.User;
import com.traintrack.coreapi.domain.UserStatus;
import com.traintrack.coreapi.security.AuthenticatedUser;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CertificationServiceTest {

    @Mock
    private CertificationRepository certificationRepository;

    @Mock
    private CertificateStorageService certificateStorageService;

    private CertificationService certificationService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        certificationService = new CertificationService(certificationRepository, certificateStorageService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generateDownloadUrlWorksForAnyCertificateWithViewAll() throws Exception {
        UUID certId = UUID.randomUUID();
        Certification certification = certificationWithKey("certificates/key.pdf");
        authenticateAs(UUID.randomUUID(), Set.of("CERT_VIEW_ALL"));
        when(certificationRepository.findByIdScoped(certId)).thenReturn(Optional.of(certification));
        URL presignedUrl = URI.create("https://s3.example.com/signed").toURL();
        when(certificateStorageService.presignDownloadUrl("certificates/key.pdf")).thenReturn(presignedUrl);
        when(certificateStorageService.presignedUrlTtl()).thenReturn(Duration.ofMinutes(15));

        CertificateDownloadResponse response = certificationService.generateDownloadUrl(certId);

        assertThat(response.url()).isEqualTo(presignedUrl.toString());
        assertThat(response.expiresAt()).isAfter(Instant.now());
    }

    @Test
    void generateDownloadUrlWorksForOwnCertificateWithViewOwnOnly() throws Exception {
        UUID certId = UUID.randomUUID();
        Certification certification = certificationWithKey("certificates/key.pdf");
        UUID ownerId = certification.getUser().getId();
        authenticateAs(ownerId, Set.of("CERT_VIEW_OWN"));
        when(certificationRepository.findByIdScoped(certId)).thenReturn(Optional.of(certification));
        URL presignedUrl = URI.create("https://s3.example.com/signed").toURL();
        when(certificateStorageService.presignDownloadUrl("certificates/key.pdf")).thenReturn(presignedUrl);
        when(certificateStorageService.presignedUrlTtl()).thenReturn(Duration.ofMinutes(15));

        CertificateDownloadResponse response = certificationService.generateDownloadUrl(certId);

        assertThat(response.url()).isEqualTo(presignedUrl.toString());
    }

    @Test
    void generateDownloadUrlDeniesAnotherUsersCertificateWithViewOwnOnly() {
        UUID certId = UUID.randomUUID();
        Certification certification = certificationWithKey("certificates/key.pdf");
        authenticateAs(UUID.randomUUID(), Set.of("CERT_VIEW_OWN"));
        when(certificationRepository.findByIdScoped(certId)).thenReturn(Optional.of(certification));

        assertThatThrownBy(() -> certificationService.generateDownloadUrl(certId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void generateDownloadUrlThrowsNotFoundWhenCertificationMissing() {
        UUID certId = UUID.randomUUID();
        authenticateAs(UUID.randomUUID(), Set.of("CERT_VIEW_ALL"));
        when(certificationRepository.findByIdScoped(certId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> certificationService.generateDownloadUrl(certId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void generateDownloadUrlThrowsNotFoundWhenNoDocumentUploadedYet() {
        UUID certId = UUID.randomUUID();
        Certification certification = certificationWithKey(null);
        authenticateAs(UUID.randomUUID(), Set.of("CERT_VIEW_ALL"));
        when(certificationRepository.findByIdScoped(certId)).thenReturn(Optional.of(certification));

        assertThatThrownBy(() -> certificationService.generateDownloadUrl(certId)).isInstanceOf(NotFoundException.class);
    }

    private void authenticateAs(UUID userId, Set<String> permissions) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, UUID.randomUUID(), "user@acme.test", permissions);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private Certification certificationWithKey(String certificateKey) {
        Organisation org = new Organisation("Acme");
        User user = new User(org, "e@acme.test", "hash", "Emp", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        Course course = new Course(org, "Fire Safety", "d", 4, 12, CourseStatus.PUBLISHED);
        Certification certification = new Certification(
                UUID.randomUUID(), user, course, Instant.now(), Instant.now().plusSeconds(3600), CertificationStatus.ACTIVE);
        certification.setCertificateUrl(certificateKey);
        return certification;
    }
}
