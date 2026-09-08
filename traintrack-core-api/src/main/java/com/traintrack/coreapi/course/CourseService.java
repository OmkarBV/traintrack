package com.traintrack.coreapi.course;

import com.traintrack.coreapi.common.exception.NotFoundException;
import com.traintrack.coreapi.course.dto.CourseCreateRequest;
import com.traintrack.coreapi.course.dto.CourseUpdateRequest;
import com.traintrack.coreapi.domain.Course;
import com.traintrack.coreapi.domain.CourseStatus;
import com.traintrack.coreapi.domain.Organisation;
import com.traintrack.coreapi.organisation.OrganisationRepository;
import com.traintrack.coreapi.security.CurrentUser;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final OrganisationRepository organisationRepository;

    public CourseService(CourseRepository courseRepository, OrganisationRepository organisationRepository) {
        this.courseRepository = courseRepository;
        this.organisationRepository = organisationRepository;
    }

    @Transactional
    public Course create(CourseCreateRequest request) {
        Organisation organisation = organisationRepository.getReferenceById(CurrentUser.requireOrgId());
        Course course = new Course(
                organisation,
                request.title(),
                request.description(),
                request.durationHours(),
                request.validityMonths(),
                CourseStatus.DRAFT);
        return courseRepository.save(course);
    }

    @Transactional(readOnly = true)
    public Page<Course> list(Pageable pageable) {
        return courseRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Course getById(UUID id) {
        return courseRepository.findByIdScoped(id).orElseThrow(() -> new NotFoundException("Course not found: " + id));
    }

    @Transactional
    public Course update(UUID id, CourseUpdateRequest request) {
        Course course = getById(id);
        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setDurationHours(request.durationHours());
        course.setValidityMonths(request.validityMonths());
        course.setStatus(request.status());
        return course;
    }

    @Transactional
    public void delete(UUID id) {
        courseRepository.delete(getById(id));
    }
}
