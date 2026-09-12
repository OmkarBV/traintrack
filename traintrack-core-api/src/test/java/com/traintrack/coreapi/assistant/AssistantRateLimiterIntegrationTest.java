package com.traintrack.coreapi.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import com.traintrack.coreapi.support.AbstractIntegrationTest;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Proves the concurrency guarantee {@link AssistantRateLimiter}'s Javadoc
 * claims, which a mocked-{@code JdbcTemplate} unit test can assert but can't
 * actually verify: many concurrent requests for the same user, against a
 * real Postgres, land on exactly the right count with none lost to a
 * read-then-write race — the entire reason the increment is one atomic
 * {@code INSERT ... ON CONFLICT ... RETURNING} rather than a separate
 * SELECT followed by an UPDATE.
 */
class AssistantRateLimiterIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AssistantRateLimiter rateLimiter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void assistantProperties(DynamicPropertyRegistry registry) {
        // High enough that this test is purely about the final count being
        // correct, not about the reject-over-quota path — that's covered
        // separately (live, during Phase 8's verification, and could be a
        // simple sequential unit test on its own).
        registry.add("traintrack.assistant.rate-limit.max-requests", () -> "1000");
        registry.add("traintrack.assistant.rate-limit.window", () -> "60m");
    }

    @Test
    void concurrentIncrementsForTheSameUserAllCountExactlyOnce() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        int concurrentRequests = 30;

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Callable<Void>> tasks = IntStream.range(0, concurrentRequests)
                .<Callable<Void>>mapToObj(i -> () -> {
                    rateLimiter.checkAndIncrement(userId, orgId);
                    return null;
                })
                .collect(Collectors.toList());

        List<Future<Void>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        for (Future<Void> future : futures) {
            future.get();
        }

        Integer requestCount = jdbcTemplate.queryForObject(
                "select request_count from assistant_rate_limits where user_id = ?", Integer.class, userId);
        assertThat(requestCount).isEqualTo(concurrentRequests);
    }
}
