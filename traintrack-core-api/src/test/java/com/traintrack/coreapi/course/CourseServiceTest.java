package com.traintrack.coreapi.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.traintrack.coreapi.common.exception.NotFoundException;
import com.traintrack.coreapi.course.dto.CourseCreateRequest;
import com.traintrack.coreapi.course.dto.CourseUpdateRequest;
import com.traintrack.coreapi.domain.Course;
import com.traintrack.coreapi.domain.CourseStatus;
import com.traintrack.coreapi.domain.Organisation;
import com.traintrack.coreapi.organisation.OrganisationRepository;
import com.traintrack.coreapi.security.AuthenticatedUser;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private OrganisationRepository organisationRepository;

    private CourseService courseService;
    private final UUID orgId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, organisationRepository);
        AuthenticatedUser principal =
                new AuthenticatedUser(UUID.randomUUID(), orgId, "trainer@acme.test", Set.of("COURSE_CREATE"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAssignsCurrentOrgAndDefaultsToDraft() {
        Organisation org = new Organisation("Acme");
        when(organisationRepository.getReferenceById(orgId)).thenReturn(org);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        Course created = courseService.create(new CourseCreateRequest("Fire Safety", "desc", 4, 12));

        assertThat(created.getOrganisation()).isSameAs(org);
        assertThat(created.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(created.getTitle()).isEqualTo("Fire Safety");
    }

    @Test
    void getByIdThrowsNotFoundWhenMissing() {
        UUID id = UUID.randomUUID();
        when(courseRepository.findByIdScoped(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getById(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateMutatesAllFields() {
        Course course = new Course(new Organisation("Acme"), "Old", "old desc", 1, 1, CourseStatus.DRAFT);
        UUID id = UUID.randomUUID();
        when(courseRepository.findByIdScoped(id)).thenReturn(Optional.of(course));

        Course updated =
                courseService.update(id, new CourseUpdateRequest("New", "new desc", 5, 24, CourseStatus.PUBLISHED));

        assertThat(updated.getTitle()).isEqualTo("New");
        assertThat(updated.getDescription()).isEqualTo("new desc");
        assertThat(updated.getDurationHours()).isEqualTo(5);
        assertThat(updated.getValidityMonths()).isEqualTo(24);
        assertThat(updated.getStatus()).isEqualTo(CourseStatus.PUBLISHED);
    }

    @Test
    void deleteRemovesTheCourse() {
        Course course = new Course(new Organisation("Acme"), "Old", "old desc", 1, 1, CourseStatus.DRAFT);
        UUID id = UUID.randomUUID();
        when(courseRepository.findByIdScoped(id)).thenReturn(Optional.of(course));

        courseService.delete(id);

        verify(courseRepository).delete(course);
    }
}
