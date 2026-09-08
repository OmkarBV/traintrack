package com.traintrack.coreapi.certification;

import com.traintrack.coreapi.certification.dto.CertificationResponse;
import com.traintrack.coreapi.domain.Certification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CertificationMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "courseId", source = "course.id")
    CertificationResponse toResponse(Certification certification);
}
