package com.traintrack.coreapi.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.traintrack.coreapi.assistant.dto.AssistantChatRequest;
import com.traintrack.coreapi.assistant.dto.AssistantChatResponse;
import com.traintrack.coreapi.assistant.dto.ChatTurn;
import com.traintrack.coreapi.security.CurrentUser;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Runs the Anthropic tool-use loop: send the conversation plus tool
 * definitions, and if the model asks to use a tool, execute it locally and
 * feed the result back — repeating until the model produces a final text
 * answer instead of another tool request.
 *
 * <p>Every {@link AssistantTool} enforces its own permission check against
 * the caller's real authorities (see the {@code assistant.tool} package),
 * not a mock or a simplified stand-in — asking "what are my certifications?"
 * and "what's expiring org-wide?" go through {@code CertificationService}
 * exactly as the corresponding REST endpoints do. A tool that denies access
 * doesn't fail the request; it becomes a {@code tool_result} the model can
 * read and explain to the caller in plain language.
 */
@Service
public class AssistantService {

    private static final String SYSTEM_PROMPT =
            """
            You are TrainTrack's training assistant. Answer questions about the
            caller's courses, enrolments, and certifications using only the
            tools provided to you — never invent data you weren't given by a
            tool. If a tool result says the caller lacks permission for
            something, say so plainly rather than guessing at an answer from
            another tool.
            """;

    /**
     * A hard ceiling on the tool-use loop itself, independent of the
     * caller-facing rate limiter: without it, a model that kept requesting
     * tools indefinitely would turn one HTTP request into an unbounded
     * number of billed Anthropic API calls.
     */
    private static final int MAX_TOOL_ITERATIONS = 5;

    private final AnthropicClient anthropicClient;
    private final AssistantRateLimiter rateLimiter;
    private final AssistantProperties properties;
    private final List<AssistantTool> tools;
    private final ObjectMapper objectMapper;

    public AssistantService(
            AnthropicClient anthropicClient,
            AssistantRateLimiter rateLimiter,
            AssistantProperties properties,
            List<AssistantTool> tools,
            ObjectMapper objectMapper) {
        this.anthropicClient = anthropicClient;
        this.rateLimiter = rateLimiter;
        this.properties = properties;
        this.tools = tools;
        this.objectMapper = objectMapper;
    }

    public AssistantChatResponse chat(AssistantChatRequest request) {
        var caller =
                CurrentUser.get().orElseThrow(() -> new IllegalStateException("No authenticated user in context"));
        // Checked before the first Anthropic call, deliberately: rejecting a
        // request over quota should never spend a token on it.
        rateLimiter.checkAndIncrement(caller.userId(), caller.orgId());

        ArrayNode messages = objectMapper.createArrayNode();
        for (ChatTurn turn : request.history()) {
            messages.add(textMessage(turn.role(), turn.content()));
        }
        messages.add(textMessage("user", request.message()));

        for (int i = 0; i < MAX_TOOL_ITERATIONS; i++) {
            JsonNode response = anthropicClient.createMessage(buildRequestBody(messages));
            JsonNode content = response.get("content");

            ObjectNode assistantMessage = objectMapper.createObjectNode();
            assistantMessage.put("role", "assistant");
            assistantMessage.set("content", content);
            messages.add(assistantMessage);

            if (!"tool_use".equals(response.path("stop_reason").asText())) {
                return new AssistantChatResponse(extractText(content));
            }

            ArrayNode toolResults = objectMapper.createArrayNode();
            for (JsonNode block : content) {
                if ("tool_use".equals(block.path("type").asText())) {
                    toolResults.add(runTool(block));
                }
            }
            ObjectNode toolResultMessage = objectMapper.createObjectNode();
            toolResultMessage.put("role", "user");
            toolResultMessage.set("content", toolResults);
            messages.add(toolResultMessage);
        }

        throw new IllegalStateException("Assistant did not produce a final answer within " + MAX_TOOL_ITERATIONS
                + " tool-use turns");
    }

    private ObjectNode buildRequestBody(ArrayNode messages) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.model());
        body.put("max_tokens", properties.maxTokens());
        body.put("system", SYSTEM_PROMPT);
        body.set("messages", messages);

        ArrayNode toolDefs = objectMapper.createArrayNode();
        for (AssistantTool tool : tools) {
            ObjectNode def = objectMapper.createObjectNode();
            def.put("name", tool.name());
            def.put("description", tool.description());
            def.set("input_schema", objectMapper.valueToTree(tool.inputSchema()));
            toolDefs.add(def);
        }
        body.set("tools", toolDefs);
        return body;
    }

    private ObjectNode textMessage(String role, String text) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", role);
        message.put("content", text);
        return message;
    }

    private ObjectNode runTool(JsonNode toolUseBlock) {
        String toolName = toolUseBlock.path("name").asText();
        String toolUseId = toolUseBlock.path("id").asText();
        JsonNode input = toolUseBlock.path("input");

        ObjectNode result = objectMapper.createObjectNode();
        result.put("type", "tool_result");
        result.put("tool_use_id", toolUseId);

        try {
            AssistantTool tool = tools.stream()
                    .filter(t -> t.name().equals(toolName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown tool: " + toolName));
            result.put("content", toJson(tool.execute(input)));
        } catch (AccessDeniedException e) {
            result.put("content", "Permission denied: " + e.getMessage());
            result.put("is_error", true);
        } catch (RuntimeException e) {
            result.put("content", "Tool failed: " + e.getMessage());
            result.put("is_error", true);
        }
        return result;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialise tool result", e);
        }
    }

    private String extractText(JsonNode content) {
        StringBuilder text = new StringBuilder();
        for (JsonNode block : content) {
            if ("text".equals(block.path("type").asText())) {
                text.append(block.path("text").asText());
            }
        }
        return text.toString();
    }
}
