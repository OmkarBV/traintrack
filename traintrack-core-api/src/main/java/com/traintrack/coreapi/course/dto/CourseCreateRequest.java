package com.traintrack.coreapi.course.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CourseCreateRequest(
        @NotBlank String title,
        String description,
        @NotNull @Min(1) Integer durationHours,
        @NotNull @Min(1) Integer validityMonths) {
}
