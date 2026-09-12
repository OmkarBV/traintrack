package com.traintrack.coreapi.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.traintrack.coreapi.common.exception.AssistantUnavailableException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * A thin wrapper around Anthropic's Messages API
 * (https://docs.anthropic.com/en/api/messages), talked to directly over HTTP
 * rather than through the official SDK. This integration needs exactly one
 * operation — create a message, optionally with tool definitions — and
 * Anthropic's content blocks are a tagged union (text / tool_use /
 * tool_result) that's simpler to build and read as raw JSON at a single call
 * site than to model as a full typed class hierarchy for one endpoint.
 */
@Component
public class AnthropicClient {

    private final RestClient restClient;

    public AnthropicClient(RestClient.Builder restClientBuilder, AssistantProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.anthropic.com")
                .defaultHeader("x-api-key", properties.anthropicApiKey())
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
    }

    /**
     * Any failure here — Anthropic rejecting the API key, a transient 5xx, a
     * network timeout — is deliberately never let through as-is: an
     * unhandled {@code HttpClientErrorException} carries Anthropic's own
     * status code, and a bare 401 from that would render as if this
     * request's own authentication had failed, which is actively
     * misleading. Every failure becomes the same
     * {@link AssistantUnavailableException} (502), with the real cause
     * preserved as the exception cause for the logs.
     */
    public JsonNode createMessage(ObjectNode requestBody) {
        try {
            return restClient.post().uri("/v1/messages").body(requestBody).retrieve().body(JsonNode.class);
        } catch (RestClientException e) {
            throw new AssistantUnavailableException("The AI assistant is temporarily unavailable", e);
        }
    }
}
