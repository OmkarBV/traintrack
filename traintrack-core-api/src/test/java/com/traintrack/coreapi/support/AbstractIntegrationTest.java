package com.traintrack.coreapi.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base for tests that need the whole application wired up against a real
 * Postgres — not embedded/mocked — so Flyway migrations, Hibernate mappings,
 * and the org-scoping filter are all exercised exactly as in production.
 * The container is started once and shared across all subclasses in a test
 * run via Testcontainers' JVM-wide container reuse of the static field.
 *
 * <p>Requires a Docker daemon whose {@code MinAPIVersion} is 1.32 or lower.
 * Docker Engine 29+ enforces {@code MinAPIVersion: 1.44} and rejects the
 * legacy probe request testcontainers-java/docker-java still send during
 * strategy detection (confirmed against testcontainers 1.21.4 and 2.0.5) —
 * an unresolved upstream issue, not something this project's code can work
 * around. On an affected host this test class fails at container startup
 * with "client version 1.32 is too old"; every other test in the suite is
 * unaffected.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
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
