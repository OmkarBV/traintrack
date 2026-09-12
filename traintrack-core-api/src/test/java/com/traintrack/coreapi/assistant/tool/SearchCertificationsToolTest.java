package com.traintrack.coreapi.assistant.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.traintrack.coreapi.certification.CertificationMapper;
import com.traintrack.coreapi.certification.CertificationService;
import com.traintrack.coreapi.domain.CertificationStatus;
import com.traintrack.coreapi.security.AuthenticatedUser;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class SearchCertificationsToolTest {

    @Mock
    private CertificationService certificationService;

    @Mock
    private CertificationMapper certificationMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SearchCertificationsTool tool;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void parsesFiltersAndSearchesOrgWideWhenAuthorised() {
        tool = new SearchCertificationsTool(certificationService, certificationMapper);
        authenticateAs(Set.of("CERT_VIEW_ALL"));
        Instant cutoff = Instant.parse("2027-01-01T00:00:00Z");
        when(certificationService.search(eq(CertificationStatus.EXPIRED), eq(cutoff), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        ObjectNode input = objectMapper.createObjectNode();
        input.put("status", "EXPIRED");
        input.put("expiringBefore", cutoff.toString());

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.execute(input);

        assertThat(result).containsKey("certifications");
    }

    @Test
    void treatsMissingFiltersAsNoFilter() {
        tool = new SearchCertificationsTool(certificationService, certificationMapper);
        authenticateAs(Set.of("CERT_VIEW_ALL"));
        when(certificationService.search(isNull(), isNull(), any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        tool.execute(objectMapper.createObjectNode());
    }

    @Test
    void deniesAccessWithoutOrgWideViewingPermission() {
        tool = new SearchCertificationsTool(certificationService, certificationMapper);
        authenticateAs(Set.of("CERT_VIEW_OWN"));

        assertThatThrownBy(() -> tool.execute(objectMapper.createObjectNode())).isInstanceOf(AccessDeniedException.class);
    }

    private void authenticateAs(Set<String> permissions) {
        AuthenticatedUser principal =
                new AuthenticatedUser(UUID.randomUUID(), UUID.randomUUID(), "user@acme.test", permissions);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
