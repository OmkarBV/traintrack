package com.traintrack.coreapi.config;

import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables {@code created_at}/{@code updated_at}/{@code created_by} population via
 * Spring Data JPA auditing instead of manual bookkeeping in every service method.
 *
 * <p>There is no authenticated principal yet in Phase 1, so {@code auditorAware}
 * always reports "no auditor" and created_by stays null. Phase 2 (JWT auth) will
 * replace this bean's body to pull the current user's id out of the security
 * context — the auditing wiring itself does not change.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<UUID> auditorAware() {
        return Optional::empty;
    }
}
