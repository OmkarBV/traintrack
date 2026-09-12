package com.traintrack.coreapi.assistant.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traintrack.coreapi.certification.CertificationMapper;
import com.traintrack.coreapi.certification.CertificationService;
import com.traintrack.coreapi.security.AuthenticatedUser;
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
class ListMyCertificationsToolTest {

    @Mock
    private CertificationService certificationService;

    @Mock
    private CertificationMapper certificationMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ListMyCertificationsTool tool;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsTheCallersOwnCertificationsWhenAuthorised() {
        tool = new ListMyCertificationsTool(certificationService, certificationMapper);
        UUID userId = UUID.randomUUID();
        authenticateAs(userId, Set.of("CERT_VIEW_OWN"));
        when(certificationService.findByUser(eq(userId), any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        ObjectNode input = objectMapper.createObjectNode();
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.execute(input);

        assertThat(result).containsKey("certifications");
    }

    @Test
    void deniesAccessWithoutEitherCertificationViewingPermission() {
        tool = new ListMyCertificationsTool(certificationService, certificationMapper);
        authenticateAs(UUID.randomUUID(), Set.of());

        assertThatThrownBy(() -> tool.execute(objectMapper.createObjectNode()))
                .isInstanceOf(AccessDeniedException.class);
    }

    private void authenticateAs(UUID userId, Set<String> permissions) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, UUID.randomUUID(), "user@acme.test", permissions);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
