package com.traintrack.coreapi.course.dto;

import com.traintrack.coreapi.domain.CourseStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CourseUpdateRequest(
        @NotBlank String title,
        String description,
        @NotNull @Min(1) Integer durationHours,
        @NotNull @Min(1) Integer validityMonths,
        @NotNull CourseStatus status) {
}
