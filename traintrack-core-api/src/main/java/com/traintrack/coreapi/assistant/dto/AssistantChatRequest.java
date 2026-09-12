package com.traintrack.coreapi.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Stateless by design: the caller (not core-api) holds the conversation and
 * resends it as {@code history} on every request. A persisted-conversation
 * model would need its own storage, retention policy, and access rules —
 * none of which this phase's brief asked for.
 */
public record AssistantChatRequest(@NotBlank String message, List<ChatTurn> history) {

    public AssistantChatRequest {
        if (history == null) {
            history = List.of();
        }
    }
}
