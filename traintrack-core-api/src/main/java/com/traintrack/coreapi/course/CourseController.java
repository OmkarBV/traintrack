package com.traintrack.coreapi.course;

import com.traintrack.coreapi.course.dto.CourseCreateRequest;
import com.traintrack.coreapi.course.dto.CourseResponse;
import com.traintrack.coreapi.course.dto.CourseUpdateRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseService courseService;
    private final CourseMapper courseMapper;

    public CourseController(CourseService courseService, CourseMapper courseMapper) {
        this.courseService = courseService;
        this.courseMapper = courseMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('COURSE_CREATE')")
    public CourseResponse create(@Valid @RequestBody CourseCreateRequest request) {
        return courseMapper.toResponse(courseService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('COURSE_VIEW')")
    public Page<CourseResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return courseService.list(pageable).map(courseMapper::toResponse);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COURSE_VIEW')")
    public CourseResponse getById(@PathVariable UUID id) {
        return courseMapper.toResponse(courseService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COURSE_UPDATE')")
    public CourseResponse update(@PathVariable UUID id, @Valid @RequestBody CourseUpdateRequest request) {
        return courseMapper.toResponse(courseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('COURSE_DELETE')")
    public void delete(@PathVariable UUID id) {
        courseService.delete(id);
    }
}
