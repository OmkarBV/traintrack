package com.traintrack.coreapi.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.traintrack.coreapi.common.exception.InvalidCredentialsException;
import com.traintrack.coreapi.domain.Organisation;
import com.traintrack.coreapi.domain.User;
import com.traintrack.coreapi.domain.UserStatus;
import com.traintrack.coreapi.rbac.PermissionRepository;
import com.traintrack.coreapi.security.JwtProperties;
import com.traintrack.coreapi.security.JwtService;
import com.traintrack.coreapi.user.UserRepository;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The success path (correct credentials all the way to a usable token pair)
 * is covered end-to-end by AuthOrgIsolationIntegrationTest against a real
 * database; this class covers the rejection branches in isolation.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private final JwtProperties jwtProperties = new JwtProperties(
            "unit-test-secret-must-be-at-least-256-bits-long!!", Duration.ofMinutes(15), Duration.ofDays(7));
    private final JwtService jwtService = new JwtService(jwtProperties);

    private AuthService authService() {
        return new AuthService(
                userRepository, refreshTokenRepository, permissionRepository, passwordEncoder, jwtService, jwtProperties);
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(userRepository.findByEmail("nobody@acme.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService().login("nobody@acme.test", "irrelevant"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginRejectsInactiveUser() {
        User user = user(UserStatus.INACTIVE);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService().login(user.getEmail(), "password123"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = user(UserStatus.ACTIVE);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService().login(user.getEmail(), "wrong")).isInstanceOf(InvalidCredentialsException.class);
    }

    private User user(UserStatus status) {
        Organisation org = new Organisation("Acme");
        return new User(org, "trainer@acme.test", "hashed-password", "Tom Trainer", status);
    }
}
