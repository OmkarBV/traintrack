package com.traintrack.coreapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Excludes the default {@code UserDetailsService} auto-configuration: auth is
 * fully stateless JWT (see the {@code security} package), so there is no
 * in-memory user store or {@code AuthenticationManager} to back off from.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class TraintrackCoreApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TraintrackCoreApiApplication.class, args);
    }
}
