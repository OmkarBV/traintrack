package com.traintrack.coreapi.security;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Registering a bean named {@code transactionManager} here means Spring
 * Boot's auto-configured {@code JpaTransactionManager} backs off in favour
 * of {@link TenantAwareJpaTransactionManager}, so every {@code @Transactional}
 * boundary in the app gets org filtering without any other code change.
 */
@Configuration
public class TransactionManagerConfig {

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new TenantAwareJpaTransactionManager(entityManagerFactory);
    }
}
