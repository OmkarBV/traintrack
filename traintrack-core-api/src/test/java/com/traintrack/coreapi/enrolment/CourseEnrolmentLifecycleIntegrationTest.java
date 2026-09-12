package com.traintrack.coreapi.enrolment;

import static org.assertj.core.api.Assertions.assertThat;

import com.traintrack.coreapi.auth.dto.LoginRequest;
import com.traintrack.coreapi.auth.dto.TokenResponse;
import com.traintrack.coreapi.course.dto.CourseCreateRequest;
import com.traintrack.coreapi.course.dto.CourseResponse;
import com.traintrack.coreapi.domain.CourseStatus;
import com.traintrack.coreapi.domain.EnrolmentStatus;
import com.traintrack.coreapi.enrolment.dto.EnrolmentCreateRequest;
import com.traintrack.coreapi.enrolment.dto.EnrolmentResponse;
import com.traintrack.coreapi.support.AbstractIntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * A fuller end-to-end scenario than {@code AuthOrgIsolationIntegrationTest}
 * covers: a real course-creation-then-enrolment business flow, over real
 * HTTP, against a real Postgres (Testcontainers), asserting the tenant
 * isolation Phase 2 established generalises correctly to Phase 3's entities
 * (courses, enrolments) — not just the Users it was originally proven
 * against. Stops short of completing the enrolment (which uploads a
 * certificate PDF to S3, requiring a LocalStack container this base class
 * doesn't provision) — that path is covered live in the Phase 7 README
 * section instead.
 */
class CourseEnrolmentLifecycleIntegrationTest extends AbstractIntegrationTest {

    private static final String ACME_ADMIN_EMAIL = "admin@acme.test";
    private static final String ACME_EMPLOYEE_ID = "d0000000-0000-0000-0000-000000000003";
    private static final String BETA_ADMIN_EMAIL = "admin@beta.test";
    private static final String PASSWORD = "password123";

    @Test
    void aCourseCreatedByOneOrgIsInvisibleToAnother() {
        String acmeToken = login(ACME_ADMIN_EMAIL);
        String betaToken = login(BETA_ADMIN_EMAIL);

        CourseResponse acmeCourse = createCourse(acmeToken, "Acme-Only Course");

        List<CourseResponse> betaCourses = listCourses(betaToken);
        assertThat(betaCourses).extracting(CourseResponse::id).doesNotContain(acmeCourse.id());

        List<CourseResponse> acmeCourses = listCourses(acmeToken);
        assertThat(acmeCourses).extracting(CourseResponse::id).contains(acmeCourse.id());
    }

    @Test
    void enrollingAUserCreatesAnEnrolmentVisibleOnlyWithinItsOrg() {
        String acmeToken = login(ACME_ADMIN_EMAIL);
        CourseResponse course = createCourse(acmeToken, "Fire Safety Refresher");

        EnrolmentResponse enrolment = enrol(acmeToken, ACME_EMPLOYEE_ID, course.id(), "lifecycle-test-" + UUID.randomUUID());

        assertThat(enrolment.status()).isEqualTo(EnrolmentStatus.ENROLLED);
        assertThat(enrolment.userId()).isEqualTo(UUID.fromString(ACME_EMPLOYEE_ID));
        assertThat(enrolment.courseId()).isEqualTo(course.id());
    }

    @Test
    void enrollingWithTheSameIdempotencyKeyTwiceReturnsTheSameEnrolmentBothTimes() {
        String acmeToken = login(ACME_ADMIN_EMAIL);
        CourseResponse course = createCourse(acmeToken, "Idempotency Check Course");
        String idempotencyKey = "lifecycle-idempotency-" + UUID.randomUUID();

        EnrolmentResponse first = enrol(acmeToken, ACME_EMPLOYEE_ID, course.id(), idempotencyKey);
        EnrolmentResponse second = enrol(acmeToken, ACME_EMPLOYEE_ID, course.id(), idempotencyKey);

        assertThat(second.id()).isEqualTo(first.id());
    }

    @Test
    void enrollingAUserFromAnotherOrgIsRejected() {
        String acmeToken = login(ACME_ADMIN_EMAIL);
        CourseResponse acmeCourse = createCourse(acmeToken, "Cross-Org Guard Course");

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl("/api/v1/enrolments"),
                HttpMethod.POST,
                authorized(
                        acmeToken,
                        new EnrolmentCreateRequest(UUID.fromString("d0000000-0000-0000-0000-000000000004"), acmeCourse.id()),
                        "lifecycle-cross-org-" + UUID.randomUUID()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private CourseResponse createCourse(String token, String title) {
        ResponseEntity<CourseResponse> response = restTemplate.exchange(
                baseUrl("/api/v1/courses"),
                HttpMethod.POST,
                authorized(token, new CourseCreateRequest(title, "Integration test course", 4, 12)),
                CourseResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(CourseStatus.DRAFT);
        return response.getBody();
    }

    private List<CourseResponse> listCourses(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<PagedCourses> response = restTemplate.exchange(
                baseUrl("/api/v1/courses"), HttpMethod.GET, new HttpEntity<>(headers), PagedCourses.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().content();
    }

    private EnrolmentResponse enrol(String token, String userId, UUID courseId, String idempotencyKey) {
        ResponseEntity<EnrolmentResponse> response = restTemplate.exchange(
                baseUrl("/api/v1/enrolments"),
                HttpMethod.POST,
                authorized(token, new EnrolmentCreateRequest(UUID.fromString(userId), courseId), idempotencyKey),
                EnrolmentResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private String login(String email) {
        ResponseEntity<TokenResponse> response = restTemplate.postForEntity(
                baseUrl("/api/v1/auth/login"), new LoginRequest(email, PASSWORD), TokenResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().accessToken();
    }

    private <T> HttpEntity<T> authorized(String token, T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    private <T> HttpEntity<T> authorized(String token, T body, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Idempotency-Key", idempotencyKey);
        return new HttpEntity<>(body, headers);
    }

    /** {@code TestRestTemplate} needs a concrete type to deserialise a page of courses into. */
    private record PagedCourses(List<CourseResponse> content) {
    }
}
