package com.traintrack.coreapi.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base for tests that need the whole application wired up against a real
 * Postgres — not embedded/mocked — so Flyway migrations, Hibernate mappings,
 * and the org-scoping filter are all exercised exactly as in production.
 * Each concrete subclass gets its own container (started fresh for that
 * class, stopped when its tests finish) rather than one container shared
 * across every integration test class in the run.
 *
 * <p>{@code @DirtiesContext(AFTER_CLASS)} is required, not optional, given
 * that per-class container lifecycle: Spring's test context cache would
 * otherwise happily keep a subclass's {@code ApplicationContext} alive
 * (backing bean pool, and everything scheduled through
 * {@code @EnableScheduling} — {@code OutboxRelay}'s 5-second sweep,
 * ShedLock's cron evaluation) after JUnit's {@code @Testcontainers}
 * extension has already stopped that class's Postgres container in its own
 * {@code afterAll}. Confirmed live in CI, not just reasoned through: without
 * this annotation, exactly that happened — the first integration test
 * class's container was torn down while its context's scheduled tasks kept
 * running against it every 5 seconds for the rest of the entire test run,
 * each one failing with "terminating connection due to unexpected postmaster
 * exit" and burning enough CPU on a 2-core CI runner that later test classes'
 * otherwise-healthy connections started timing out too. Forcing Spring to
 * close the context (and everything scheduled through it) at the end of
 * each class keeps a class's background tasks alive for exactly as long as
 * its own container is.
 *
 * <p>Requires a Docker daemon whose {@code MinAPIVersion} is 1.32 or lower.
 * Docker Engine 29+ enforces {@code MinAPIVersion: 1.44} and rejects the
 * legacy probe request testcontainers-java/docker-java still send during
 * strategy detection (confirmed against testcontainers 1.21.4 and 2.0.5) —
 * an unresolved upstream issue, not something this project's code can work
 * around. On an affected host this test class fails at container startup
 * with "client version 1.32 is too old"; every other test in the suite is
 * unaffected. GitHub Actions' hosted runners don't have this problem.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("traintrack_core");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @LocalServerPort
    protected int port;

    protected TestRestTemplate restTemplate = new TestRestTemplate();

    protected String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }
}
