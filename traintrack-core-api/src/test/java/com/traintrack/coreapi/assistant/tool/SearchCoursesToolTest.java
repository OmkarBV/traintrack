package com.traintrack.coreapi.assistant.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traintrack.coreapi.course.CourseMapper;
import com.traintrack.coreapi.course.CourseService;
import com.traintrack.coreapi.security.AuthenticatedUser;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class SearchCoursesToolTest {

    @Mock
    private CourseService courseService;

    @Mock
    private CourseMapper courseMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SearchCoursesTool tool;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsTheCatalogWhenAuthorised() {
        tool = new SearchCoursesTool(courseService, courseMapper);
        authenticateAs(Set.of("COURSE_VIEW"));
        when(courseService.list(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) tool.execute(objectMapper.createObjectNode());

        assertThat(result).containsKey("courses");
    }

    @Test
    void deniesAccessWithoutCourseViewPermission() {
        tool = new SearchCoursesTool(courseService, courseMapper);
        authenticateAs(Set.of());

        assertThatThrownBy(() -> tool.execute(objectMapper.createObjectNode())).isInstanceOf(AccessDeniedException.class);
    }

    private void authenticateAs(Set<String> permissions) {
        AuthenticatedUser principal =
                new AuthenticatedUser(UUID.randomUUID(), UUID.randomUUID(), "user@acme.test", permissions);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
