package com.traintrack.coreapi.user;

import com.traintrack.coreapi.domain.User;
import com.traintrack.coreapi.user.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "orgId", source = "organisation.id")
    UserResponse toResponse(User user);
}
