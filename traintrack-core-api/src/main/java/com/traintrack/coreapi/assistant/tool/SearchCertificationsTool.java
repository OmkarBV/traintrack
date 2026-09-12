package com.traintrack.coreapi.assistant.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.traintrack.coreapi.assistant.AssistantTool;
import com.traintrack.coreapi.certification.CertificationMapper;
import com.traintrack.coreapi.certification.CertificationService;
import com.traintrack.coreapi.certification.dto.CertificationResponse;
import com.traintrack.coreapi.domain.CertificationStatus;
import com.traintrack.coreapi.security.CurrentUser;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class SearchCertificationsTool implements AssistantTool {

    private final CertificationService certificationService;
    private final CertificationMapper certificationMapper;

    public SearchCertificationsTool(CertificationService certificationService, CertificationMapper certificationMapper) {
        this.certificationService = certificationService;
        this.certificationMapper = certificationMapper;
    }

    @Override
    public String name() {
        return "search_certifications";
    }

    @Override
    public String description() {
        return "Searches certifications across the whole organisation, optionally filtered by status and/or an "
                + "expiry cutoff. Requires organisation-wide certificate visibility, not available to every caller — "
                + "if it's not available, tell the user plainly rather than guessing at an answer from other tools.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
                "type",
                "object",
                "properties",
                Map.of(
                        "status",
                        Map.of(
                                "type", "string",
                                "enum", List.of("ACTIVE", "EXPIRED", "REVOKED"),
                                "description", "Filter by certification status"),
                        "expiringBefore",
                        Map.of(
                                "type", "string",
                                "format", "date-time",
                                "description", "Only include certifications expiring before this ISO-8601 instant")),
                "required",
                List.of());
    }

    @Override
    public Object execute(JsonNode input) {
        var caller = CurrentUser.get().orElseThrow(() -> new AccessDeniedException("No authenticated user"));
        if (!caller.permissions().contains("CERT_VIEW_ALL")) {
            throw new AccessDeniedException("Missing CERT_VIEW_ALL permission");
        }

        CertificationStatus status =
                input.hasNonNull("status") ? CertificationStatus.valueOf(input.get("status").asText()) : null;
        Instant expiringBefore =
                input.hasNonNull("expiringBefore") ? Instant.parse(input.get("expiringBefore").asText()) : null;

        List<CertificationResponse> certifications = certificationService
                .search(status, expiringBefore, PageRequest.of(0, 50))
                .map(certificationMapper::toResponse)
                .getContent();
        return Map.of("certifications", certifications);
    }
}
