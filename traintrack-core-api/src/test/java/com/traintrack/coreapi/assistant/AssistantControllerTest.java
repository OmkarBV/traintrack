package com.traintrack.coreapi.assistant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.traintrack.coreapi.assistant.dto.AssistantChatResponse;
import com.traintrack.coreapi.common.exception.AssistantUnavailableException;
import com.traintrack.coreapi.common.exception.RateLimitExceededException;
import com.traintrack.coreapi.support.TestAuthentications;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Covers the web layer around {@link AssistantService}: the endpoint is open
 * to any authenticated caller (permission checks live per-tool, not here —
 * see Phase 8's README section), request validation, and — the two cases
 * exercised manually with curl during Phase 8's live verification, now
 * pinned as regression tests — that {@link RateLimitExceededException} and
 * {@link AssistantUnavailableException} map to 429 and 502 respectively,
 * rather than leaking through as an unhandled 500.
 */
@WebMvcTest(AssistantController.class)
@Import(AssistantControllerTest.MethodSecurityConfig.class)
class AssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssistantService assistantService;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/assistant/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsABlankMessage() throws Exception {
        mockMvc.perform(post("/api/v1/assistant/messages")
                        .with(authentication(TestAuthentications.withPermissions(Set.of())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsTheAssistantsReplyOnSuccess() throws Exception {
        when(assistantService.chat(any())).thenReturn(new AssistantChatResponse("Hello!"));

        mockMvc.perform(post("/api/v1/assistant/messages")
                        .with(authentication(TestAuthentications.withPermissions(Set.of())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Hello!"));
    }

    @Test
    void mapsRateLimitExceededTo429() throws Exception {
        when(assistantService.chat(any())).thenThrow(new RateLimitExceededException("Rate limit exceeded"));

        mockMvc.perform(post("/api/v1/assistant/messages")
                        .with(authentication(TestAuthentications.withPermissions(Set.of())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void mapsAssistantUnavailableTo502() throws Exception {
        when(assistantService.chat(any()))
                .thenThrow(new AssistantUnavailableException("The AI assistant is temporarily unavailable", null));

        mockMvc.perform(post("/api/v1/assistant/messages")
                        .with(authentication(TestAuthentications.withPermissions(Set.of())))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\"}"))
                .andExpect(status().isBadGateway());
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }
}
