package com.traintrack.coreapi.user.dto;

import com.traintrack.coreapi.domain.UserStatus;
import java.util.UUID;

public record UserResponse(UUID id, UUID orgId, String email, String fullName, UserStatus status) {
}
