package com.traintrack.coreapi.assistant;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "traintrack.assistant")
public record AssistantProperties(String anthropicApiKey, String model, int maxTokens, RateLimit rateLimit) {

    public record RateLimit(int maxRequests, Duration window) {
    }
}
