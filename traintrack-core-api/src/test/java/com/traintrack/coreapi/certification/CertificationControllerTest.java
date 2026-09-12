package com.traintrack.coreapi.certification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.traintrack.coreapi.certification.dto.CertificateDownloadResponse;
import com.traintrack.coreapi.common.exception.NotFoundException;
import com.traintrack.coreapi.support.TestAuthentications;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The download-url endpoint (Phase 7) splits its authorisation check across
 * two layers: a coarse {@code @PreAuthorize} gate here, and a fine-grained
 * per-certificate ownership check inside the (mocked) service — see
 * {@link CertificationController#downloadUrl}. These tests cover the first
 * layer directly and confirm an {@link AccessDeniedException} thrown from
 * the second layer still translates to 403 the same way a {@code @PreAuthorize}
 * failure would.
 */
@WebMvcTest(CertificationController.class)
@Import(CertificationControllerTest.MethodSecurityConfig.class)
class CertificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CertificationService certificationService;

    @MockBean
    private CertificationMapper certificationMapper;

    @Test
    void searchRequiresCertViewAllPermission() throws Exception {
        mockMvc.perform(
                        get("/api/v1/certifications").with(authentication(TestAuthentications.withPermissions(Set.of()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void searchSucceedsWithCertViewAllPermission() throws Exception {
        when(certificationService.search(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/certifications")
                        .with(authentication(TestAuthentications.withPermissions(Set.of("CERT_VIEW_ALL")))))
                .andExpect(status().isOk());
    }

    @Test
    void byUserAllowsACallerToRequestTheirOwnIdWithViewOwnOnly() throws Exception {
        UUID userId = UUID.randomUUID();
        when(certificationService.findByUser(any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/users/" + userId + "/certifications")
                        .with(authentication(
                                TestAuthentications.withPermissions(userId, UUID.randomUUID(), Set.of("CERT_VIEW_OWN")))))
                .andExpect(status().isOk());
    }

    @Test
    void byUserDeniesACallerRequestingSomeoneElsesIdWithViewOwnOnly() throws Exception {
        UUID someoneElsesId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/users/" + someoneElsesId + "/certifications")
                        .with(authentication(TestAuthentications.withPermissions(Set.of("CERT_VIEW_OWN")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void byUserAllowsAnyIdWithViewAllPermission() throws Exception {
        when(certificationService.findByUser(any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/users/" + UUID.randomUUID() + "/certifications")
                        .with(authentication(TestAuthentications.withPermissions(Set.of("CERT_VIEW_ALL")))))
                .andExpect(status().isOk());
    }

    @Test
    void downloadUrlRequiresAtLeastOneCertPermission() throws Exception {
        mockMvc.perform(get("/api/v1/certifications/" + UUID.randomUUID() + "/download-url")
                        .with(authentication(TestAuthentications.withPermissions(Set.of()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void downloadUrlReturnsTheServicesPresignedUrlWhenAuthorised() throws Exception {
        UUID certId = UUID.randomUUID();
        when(certificationService.generateDownloadUrl(certId))
                .thenReturn(new CertificateDownloadResponse("https://example.test/signed", Instant.now()));

        mockMvc.perform(get("/api/v1/certifications/" + certId + "/download-url")
                        .with(authentication(TestAuthentications.withPermissions(Set.of("CERT_VIEW_OWN")))))
                .andExpect(status().isOk());
    }

    @Test
    void downloadUrlPropagatesA404FromTheServiceWhenTheCertificationDoesNotExist() throws Exception {
        UUID certId = UUID.randomUUID();
        when(certificationService.generateDownloadUrl(certId))
                .thenThrow(new NotFoundException("Certification not found: " + certId));

        mockMvc.perform(get("/api/v1/certifications/" + certId + "/download-url")
                        .with(authentication(TestAuthentications.withPermissions(Set.of("CERT_VIEW_ALL")))))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadUrlTranslatesAServiceLevelOwnershipDenialTo403() throws Exception {
        UUID certId = UUID.randomUUID();
        when(certificationService.generateDownloadUrl(certId))
                .thenThrow(new AccessDeniedException("You do not have permission to download this certificate"));

        mockMvc.perform(get("/api/v1/certifications/" + certId + "/download-url")
                        .with(authentication(TestAuthentications.withPermissions(Set.of("CERT_VIEW_OWN")))))
                .andExpect(status().isForbidden());
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }
}
