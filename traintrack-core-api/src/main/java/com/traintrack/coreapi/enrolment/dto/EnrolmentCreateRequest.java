package com.traintrack.coreapi.enrolment.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EnrolmentCreateRequest(@NotNull UUID userId, @NotNull UUID courseId) {
}
