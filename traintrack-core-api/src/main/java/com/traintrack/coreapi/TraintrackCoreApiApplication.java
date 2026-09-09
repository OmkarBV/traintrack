package com.traintrack.coreapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Excludes the default {@code UserDetailsService} auto-configuration: auth is
 * fully stateless JWT (see the {@code security} package), so there is no
 * in-memory user store or {@code AuthenticationManager} to back off from.
 *
 * <p>{@code @EnableScheduling} backs OutboxRelay's sweep (Phase 4); Phase 6
 * adds the certification-expiry jobs on the same infrastructure.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
@EnableScheduling
public class TraintrackCoreApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TraintrackCoreApiApplication.class, args);
    }
}
