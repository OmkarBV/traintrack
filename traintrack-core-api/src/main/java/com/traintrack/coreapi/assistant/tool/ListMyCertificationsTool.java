package com.traintrack.coreapi.assistant.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.traintrack.coreapi.assistant.AssistantTool;
import com.traintrack.coreapi.certification.CertificationMapper;
import com.traintrack.coreapi.certification.CertificationService;
import com.traintrack.coreapi.certification.dto.CertificationResponse;
import com.traintrack.coreapi.security.CurrentUser;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class ListMyCertificationsTool implements AssistantTool {

    private final CertificationService certificationService;
    private final CertificationMapper certificationMapper;

    public ListMyCertificationsTool(CertificationService certificationService, CertificationMapper certificationMapper) {
        this.certificationService = certificationService;
        this.certificationMapper = certificationMapper;
    }

    @Override
    public String name() {
        return "list_my_certifications";
    }

    @Override
    public String description() {
        return "Lists the certifications belonging to the caller: course, status, issue date, and expiry date.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(), "required", List.of());
    }

    @Override
    public Object execute(JsonNode input) {
        var caller = CurrentUser.get().orElseThrow(() -> new AccessDeniedException("No authenticated user"));
        if (!caller.permissions().contains("CERT_VIEW_OWN") && !caller.permissions().contains("CERT_VIEW_ALL")) {
            throw new AccessDeniedException("Missing CERT_VIEW_OWN permission");
        }

        List<CertificationResponse> certifications = certificationService
                .findByUser(caller.userId(), PageRequest.of(0, 50))
                .map(certificationMapper::toResponse)
                .getContent();
        return Map.of("certifications", certifications);
    }
}
