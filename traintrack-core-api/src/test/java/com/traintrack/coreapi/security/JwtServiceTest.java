package com.traintrack.coreapi.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.traintrack.coreapi.common.exception.InvalidTokenException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            new JwtProperties("unit-test-secret-must-be-at-least-256-bits-long!!", Duration.ofMinutes(15), Duration
                    .ofDays(7)));

    @Test
    void accessTokenRoundTripsAllClaims() {
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        String token = jwtService.generateAccessToken(userId, orgId, "trainer@acme.test", Set.of("COURSE_VIEW", "COURSE_CREATE"));
        AuthenticatedUser parsed = jwtService.parseAccessToken(token);

        assertThat(parsed.userId()).isEqualTo(userId);
        assertThat(parsed.orgId()).isEqualTo(orgId);
        assertThat(parsed.email()).isEqualTo("trainer@acme.test");
        assertThat(parsed.permissions()).containsExactlyInAnyOrder("COURSE_VIEW", "COURSE_CREATE");
    }

    @Test
    void refreshTokenRoundTripsUserAndTokenId() {
        UUID userId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();

        String token = jwtService.generateRefreshToken(userId, tokenId, Instant.now().plusSeconds(60));
        JwtService.RefreshTokenClaims parsed = jwtService.parseRefreshToken(token);

        assertThat(parsed.userId()).isEqualTo(userId);
        assertThat(parsed.tokenId()).isEqualTo(tokenId);
    }

    @Test
    void rejectsAnAccessTokenPresentedAsARefreshToken() {
        String accessToken = jwtService.generateAccessToken(UUID.randomUUID(), UUID.randomUUID(), "a@b.test", Set.of());

        assertThatThrownBy(() -> jwtService.parseRefreshToken(accessToken)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rejectsARefreshTokenPresentedAsAnAccessToken() {
        String refreshToken = jwtService.generateRefreshToken(UUID.randomUUID(), UUID.randomUUID(), Instant.now().plusSeconds(60));

        assertThatThrownBy(() -> jwtService.parseAccessToken(refreshToken)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rejectsATamperedToken() {
        String token = jwtService.generateAccessToken(UUID.randomUUID(), UUID.randomUUID(), "a@b.test", Set.of());

        assertThatThrownBy(() -> jwtService.parseAccessToken(token + "tampered"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rejectsAnExpiredToken() {
        JwtService shortLived = new JwtService(
                new JwtProperties(
                        "unit-test-secret-must-be-at-least-256-bits-long!!", Duration.ofSeconds(-1), Duration.ofDays(7)));
        String token = shortLived.generateAccessToken(UUID.randomUUID(), UUID.randomUUID(), "a@b.test", Set.of());

        assertThatThrownBy(() -> shortLived.parseAccessToken(token)).isInstanceOf(InvalidTokenException.class);
    }
}
