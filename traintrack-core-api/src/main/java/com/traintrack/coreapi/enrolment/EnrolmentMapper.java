package com.traintrack.coreapi.enrolment;

import com.traintrack.coreapi.domain.Enrolment;
import com.traintrack.coreapi.enrolment.dto.EnrolmentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EnrolmentMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "courseId", source = "course.id")
    EnrolmentResponse toResponse(Enrolment enrolment);
}
