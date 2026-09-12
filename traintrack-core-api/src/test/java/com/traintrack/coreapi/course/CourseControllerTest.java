package com.traintrack.coreapi.course;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.traintrack.coreapi.course.dto.CourseResponse;
import com.traintrack.coreapi.domain.Course;
import com.traintrack.coreapi.domain.CourseStatus;
import com.traintrack.coreapi.domain.Organisation;
import com.traintrack.coreapi.support.TestAuthentications;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exercises the web layer in isolation from real business logic (the
 * service is mocked): routing, {@code @PreAuthorize} permission gates,
 * request validation, and exception-to-{@code ProblemDetail} mapping. Real
 * JWT authentication isn't loaded in this slice — {@link TestAuthentications}
 * injects an {@code Authentication} carrying our custom principal directly,
 * so {@code @PreAuthorize} still evaluates against real authorities without
 * needing a real token.
 */
@WebMvcTest(CourseController.class)
@Import(CourseControllerTest.MethodSecurityConfig.class)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseService courseService;

    @MockBean
    private CourseMapper courseMapper;

    @Test
    void listRequiresCourseViewPermission() throws Exception {
        mockMvc.perform(get("/api/v1/courses").with(authentication(TestAuthentications.withPermissions(Set.of()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listSucceedsWithCourseViewPermission() throws Exception {
        when(courseService.list(any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/courses")
                        .with(authentication(TestAuthentications.withPermissions(Set.of("COURSE_VIEW")))))
                .andExpect(status().isOk());
    }

    @Test
    void createRejectsAMissingTitleWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                        .with(authentication(TestAuthentications.withPermissions(Set.of("COURSE_CREATE"))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationHours\":4,\"validityMonths\":12}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSucceedsWithCourseCreatePermissionAndAValidBody() throws Exception {
        Organisation org = new Organisation("Acme");
        Course course = new Course(org, "Fire Safety", "d", 4, 12, CourseStatus.DRAFT);
        when(courseService.create(any())).thenReturn(course);
        when(courseMapper.toResponse(course))
                .thenReturn(new CourseResponse(null, "Fire Safety", "d", 4, 12, CourseStatus.DRAFT));

        mockMvc.perform(post("/api/v1/courses")
                        .with(authentication(TestAuthentications.withPermissions(Set.of("COURSE_CREATE"))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Fire Safety\",\"description\":\"d\",\"durationHours\":4,\"validityMonths\":12}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Fire Safety"));
    }

    @Test
    void createWithoutCourseCreatePermissionIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/courses")
                        .with(authentication(TestAuthentications.withPermissions(Set.of("COURSE_VIEW"))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Fire Safety\",\"durationHours\":4,\"validityMonths\":12}"))
                .andExpect(status().isForbidden());
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }
}
