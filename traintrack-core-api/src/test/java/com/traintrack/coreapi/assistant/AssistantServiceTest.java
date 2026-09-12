package com.traintrack.coreapi.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.traintrack.coreapi.assistant.dto.AssistantChatRequest;
import com.traintrack.coreapi.assistant.dto.AssistantChatResponse;
import com.traintrack.coreapi.common.exception.RateLimitExceededException;
import com.traintrack.coreapi.security.AuthenticatedUser;
import java.time.Duration;
import java.util.List;
import java.util.Map;
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

@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

    @Mock
    private AnthropicClient anthropicClient;

    @Mock
    private AssistantRateLimiter rateLimiter;

    @Mock
    private AssistantTool searchCoursesTool;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AssistantService assistantService;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(searchCoursesTool.name()).thenReturn("search_courses");
        AssistantProperties properties = new AssistantProperties(
                "test-key", "claude-sonnet-5", 1024, new AssistantProperties.RateLimit(20, Duration.ofHours(1)));
        assistantService = new AssistantService(
                anthropicClient, rateLimiter, properties, List.of(searchCoursesTool), objectMapper);

        AuthenticatedUser principal =
                new AuthenticatedUser(UUID.randomUUID(), UUID.randomUUID(), "user@acme.test", Set.of("COURSE_VIEW"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsTheFinalTextAnswerWhenNoToolIsNeeded() {
        when(anthropicClient.createMessage(any())).thenReturn(textResponse("end_turn", "Hello there."));

        AssistantChatResponse response = assistantService.chat(new AssistantChatRequest("hi", null));

        assertThat(response.reply()).isEqualTo("Hello there.");
        verify(searchCoursesTool, never()).execute(any());
    }

    @Test
    void executesARequestedToolAndFeedsTheResultBackForAFinalAnswer() {
        when(searchCoursesTool.execute(any())).thenReturn(Map.of("courses", List.of()));
        when(anthropicClient.createMessage(any()))
                .thenReturn(toolUseResponse("toolu_1", "search_courses", objectMapper.createObjectNode()))
                .thenReturn(textResponse("end_turn", "There are no courses yet."));

        AssistantChatResponse response =
                assistantService.chat(new AssistantChatRequest("what courses are there?", null));

        assertThat(response.reply()).isEqualTo("There are no courses yet.");
        verify(searchCoursesTool).execute(any());
    }

    @Test
    void turnsAToolPermissionDenialIntoAnErrorResultInsteadOfFailingTheRequest() {
        when(searchCoursesTool.execute(any())).thenThrow(new AccessDeniedException("Missing COURSE_VIEW permission"));
        when(anthropicClient.createMessage(any()))
                .thenReturn(toolUseResponse("toolu_1", "search_courses", objectMapper.createObjectNode()))
                .thenReturn(textResponse("end_turn", "You don't have permission to view courses."));

        AssistantChatResponse response =
                assistantService.chat(new AssistantChatRequest("what courses are there?", null));

        assertThat(response.reply()).isEqualTo("You don't have permission to view courses.");
    }

    @Test
    void checksTheRateLimiterBeforeCallingAnthropicAtAll() {
        org.mockito.Mockito.doThrow(new RateLimitExceededException("too many requests"))
                .when(rateLimiter)
                .checkAndIncrement(any(), any());

        assertThatThrownBy(() -> assistantService.chat(new AssistantChatRequest("hi", null)))
                .isInstanceOf(RateLimitExceededException.class);

        verify(anthropicClient, never()).createMessage(any());
    }

    private JsonNode textResponse(String stopReason, String text) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("stop_reason", stopReason);
        var content = response.putArray("content");
        ObjectNode block = content.addObject();
        block.put("type", "text");
        block.put("text", text);
        return response;
    }

    private JsonNode toolUseResponse(String toolUseId, String toolName, ObjectNode input) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("stop_reason", "tool_use");
        var content = response.putArray("content");
        ObjectNode block = content.addObject();
        block.put("type", "tool_use");
        block.put("id", toolUseId);
        block.put("name", toolName);
        block.set("input", input);
        return response;
    }
}
