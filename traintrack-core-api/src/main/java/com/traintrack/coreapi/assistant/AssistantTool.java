package com.traintrack.coreapi.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/**
 * One capability the assistant can invoke. Every implementation is
 * responsible for its own permission check (see the {@code assistant.tool}
 * package) — there's no single endpoint-level gate that could enforce this
 * instead, since which questions a caller can get real answers to varies
 * tool-by-tool. A permission failure should throw
 * {@link org.springframework.security.access.AccessDeniedException}; the
 * caller (AssistantService) turns that into a tool-result error the model
 * can explain, rather than failing the whole request.
 */
public interface AssistantTool {

    String name();

    String description();

    /** An Anthropic tool `input_schema`: a JSON Schema object describing the tool's parameters. */
    Map<String, Object> inputSchema();

    Object execute(JsonNode input);
}
