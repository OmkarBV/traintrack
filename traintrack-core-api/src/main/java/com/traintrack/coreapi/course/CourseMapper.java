package com.traintrack.coreapi.course;

import com.traintrack.coreapi.course.dto.CourseResponse;
import com.traintrack.coreapi.domain.Course;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    CourseResponse toResponse(Course course);
}
