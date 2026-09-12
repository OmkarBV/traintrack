package com.traintrack.coreapi.config;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Without this, every horizontally-scaled instance runs its own independent
 * in-process scheduler, and all of them would fire the same cron trigger at
 * the same wall-clock time — not a hypothetical race, but exactly what
 * happens by default. For "mark lapsed certifications EXPIRED" that's
 * wasteful but harmless (every instance's UPDATE targets the same rows;
 * whichever commits first wins, the rest no-op). For "publish
 * certification.expiring events" it's a genuine correctness bug: N
 * instances would each independently query the same not-yet-notified
 * certifications before any of them commits the notified-flag, and each
 * would publish its own duplicate event — a race the query's own idempotency
 * check can't catch, since it only prevents re-notifying *tomorrow*, not
 * concurrently *today*. ShedLock's table-row lock ensures only one instance
 * ever runs a given job at a time, so the race never starts.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build());
    }
}
