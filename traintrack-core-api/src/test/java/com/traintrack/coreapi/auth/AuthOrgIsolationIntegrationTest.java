package com.traintrack.coreapi.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.traintrack.coreapi.auth.dto.LoginRequest;
import com.traintrack.coreapi.auth.dto.RefreshRequest;
import com.traintrack.coreapi.auth.dto.TokenResponse;
import com.traintrack.coreapi.support.AbstractIntegrationTest;
import com.traintrack.coreapi.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

/**
 * Exercises the full stack (real HTTP, real Postgres via Testcontainers, the
 * actual security filter chain and Hibernate org filter) against the two
 * organisations seeded in V3__auth_phase2.sql: Acme Compliance Ltd and Beta
 * Industries.
 */
class AuthOrgIsolationIntegrationTest extends AbstractIntegrationTest {

    private static final String ACME_ADMIN_EMAIL = "admin@acme.test";
    private static final String ACME_EMPLOYEE_EMAIL = "employee@acme.test";
    private static final String ACME_TRAINER_ID = "d0000000-0000-0000-0000-000000000002";
    private static final String BETA_ADMIN_ID = "d0000000-0000-0000-0000-000000000004";
    private static final String PASSWORD = "password123";

    @Test
    void loginWithWrongPasswordReturns401() {
        ResponseEntity<ProblemDetail> response = restTemplate.postForEntity(
                baseUrl("/api/v1/auth/login"), new LoginRequest(ACME_ADMIN_EMAIL, "wrong-password"), ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void adminCanReadAUserInTheirOwnOrg() {
        String token = login(ACME_ADMIN_EMAIL).accessToken();

        ResponseEntity<UserResponse> response =
                restTemplate.exchange(baseUrl("/api/v1/users/" + ACME_TRAINER_ID), HttpMethod.GET, authorized(token), UserResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo("trainer@acme.test");
    }

    /**
     * The Phase 2 requirement this project's spec calls out explicitly: a user
     * from one org must get 404, not 403, when asking for another org's
     * resource by id. 403 would leak the fact that the row exists at all;
     * 404 makes a foreign-org id indistinguishable from a made-up one.
     */
    @Test
    void adminGetsNotFound_notForbidden_forAnotherOrgsUser() {
        String token = login(ACME_ADMIN_EMAIL).accessToken();

        ResponseEntity<ProblemDetail> response =
                restTemplate.exchange(baseUrl("/api/v1/users/" + BETA_ADMIN_ID), HttpMethod.GET, authorized(token), ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void employeeWithoutUserManagePermissionGetsForbidden() {
        String token = login(ACME_EMPLOYEE_EMAIL).accessToken();

        ResponseEntity<ProblemDetail> response = restTemplate.exchange(
                baseUrl("/api/v1/users/" + ACME_TRAINER_ID), HttpMethod.GET, authorized(token), ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void requestWithoutATokenIsUnauthorized() {
        ResponseEntity<ProblemDetail> response =
                restTemplate.getForEntity(baseUrl("/api/v1/users/me"), ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refreshTokenIsSingleUse() {
        TokenResponse tokens = login(ACME_ADMIN_EMAIL);

        ResponseEntity<TokenResponse> first = restTemplate.postForEntity(
                baseUrl("/api/v1/auth/refresh"), new RefreshRequest(tokens.refreshToken()), TokenResponse.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ProblemDetail> replay = restTemplate.postForEntity(
                baseUrl("/api/v1/auth/refresh"), new RefreshRequest(tokens.refreshToken()), ProblemDetail.class);
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private TokenResponse login(String email) {
        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                baseUrl("/api/v1/auth/login"), new LoginRequest(email, PASSWORD), TokenResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private HttpEntity<Void> authorized(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }
}
