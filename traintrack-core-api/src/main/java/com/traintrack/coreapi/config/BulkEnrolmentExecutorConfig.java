package com.traintrack.coreapi.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;

/**
 * Two executors for two different jobs, both wrapped to propagate the
 * calling thread's {@code SecurityContext} — without that, worker threads
 * would run outside any authenticated context, and
 * {@code TenantAwareJpaTransactionManager} (see the security package) would
 * silently fail to enable the org-scoping Hibernate filter, since it reads
 * the org id from {@code SecurityContextHolder}. That's not hypothetical:
 * it's exactly what happens by default, since {@code ThreadLocal}-based
 * security context does not propagate across thread pool boundaries.
 *
 * <ul>
 *   <li>{@code bulkEnrolmentExecutor} — bounded platform threads that do the
 *       actual per-row DB work. Sized relative to the JDBC connection pool
 *       (see application.yml's {@code hikari.maximum-pool-size: 15}), not
 *       CPU count: this work is I/O-bound, and more threads than available
 *       DB connections would just queue on the connection pool instead of
 *       increasing throughput. maxPoolSize=8 leaves 7 connections free for
 *       ordinary request traffic while a bulk job is running. queueCapacity
 *       is set to the endpoint's own row cap (5000) so a single batch never
 *       triggers the rejection policy; CallerRunsPolicy is still configured
 *       as backpressure for the case where multiple orgs run bulk jobs
 *       concurrently and the combined queue does fill up.</li>
 *   <li>{@code bulkCoordinatorExecutor} — virtual threads. Its only job is
 *       to submit up to 5000 row-tasks and block waiting for them all to
 *       finish; that's a cheap thing to park a virtual thread on, and using
 *       virtual threads here (rather than a second bounded platform-thread
 *       pool) avoids reserving a platform thread for the entire duration of
 *       a bulk job purely to sit idle waiting.</li>
 * </ul>
 */
@Configuration
@EnableAsync
public class BulkEnrolmentExecutorConfig {

    @Bean(name = "bulkEnrolmentExecutor")
    public Executor bulkEnrolmentExecutor() {
        ThreadPoolTaskExecutor delegate = new ThreadPoolTaskExecutor();
        delegate.setCorePoolSize(4);
        delegate.setMaxPoolSize(8);
        delegate.setQueueCapacity(5000);
        delegate.setThreadNamePrefix("bulk-enrol-");
        delegate.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        delegate.initialize();
        return new DelegatingSecurityContextExecutor(delegate);
    }

    @Bean(name = "bulkCoordinatorExecutor")
    public Executor bulkCoordinatorExecutor() {
        return new DelegatingSecurityContextExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }
}
