package com.traintrack.coreapi.assistant;

import com.traintrack.coreapi.common.exception.RateLimitExceededException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * A fixed-window request counter enforced in Postgres, not in application
 * memory: core-api runs as multiple instances (see V7's ShedLock table for
 * the same underlying reason), and an in-process counter would let each
 * instance hand out its own independent quota — a caller hitting two
 * different instances would effectively get double the configured limit.
 * The atomic {@code INSERT ... ON CONFLICT ... RETURNING} is what makes
 * concurrent requests for the same user, on different instances, count
 * correctly rather than racing on a read-then-write.
 *
 * <p>A fixed window (as opposed to a sliding one) can let a caller make up
 * to roughly twice the configured number of requests across a single window
 * boundary — accepted here as a simple, well-understood trade-off; closing
 * that gap with a sliding window or token bucket would cost a materially
 * more complex query for a difference that doesn't matter at this scale.
 */
@Component
public class AssistantRateLimiter {

    private final JdbcTemplate jdbcTemplate;
    private final AssistantProperties properties;

    public AssistantRateLimiter(JdbcTemplate jdbcTemplate, AssistantProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    public void checkAndIncrement(UUID userId, UUID orgId) {
        Instant windowStart = currentWindowStart();
        Integer count = jdbcTemplate.queryForObject(
                """
                INSERT INTO assistant_rate_limits (user_id, org_id, window_start, request_count)
                VALUES (?, ?, ?, 1)
                ON CONFLICT (user_id, window_start)
                DO UPDATE SET request_count = assistant_rate_limits.request_count + 1
                RETURNING request_count
                """,
                Integer.class,
                userId,
                orgId,
                Timestamp.from(windowStart));

        int maxRequests = properties.rateLimit().maxRequests();
        if (count != null && count > maxRequests) {
            throw new RateLimitExceededException(
                    "Rate limit exceeded: " + maxRequests + " requests per " + properties.rateLimit().window());
        }
    }

    private Instant currentWindowStart() {
        long windowSeconds = properties.rateLimit().window().getSeconds();
        long nowEpoch = Instant.now().getEpochSecond();
        return Instant.ofEpochSecond((nowEpoch / windowSeconds) * windowSeconds);
    }
}
