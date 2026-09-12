package com.traintrack.coreapi.assistant;

import com.traintrack.coreapi.assistant.dto.AssistantChatRequest;
import com.traintrack.coreapi.assistant.dto.AssistantChatResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    /**
     * Open to any authenticated user — each {@link AssistantTool} enforces
     * its own permission requirement, since which questions a caller can get
     * real answers to varies tool-by-tool, not endpoint-by-endpoint.
     */
    @PostMapping("/messages")
    @PreAuthorize("isAuthenticated()")
    public AssistantChatResponse chat(@Valid @RequestBody AssistantChatRequest request) {
        return assistantService.chat(request);
    }
}
