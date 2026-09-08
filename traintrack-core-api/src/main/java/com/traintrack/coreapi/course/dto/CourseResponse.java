package com.traintrack.coreapi.course.dto;

import com.traintrack.coreapi.domain.CourseStatus;
import java.util.UUID;

public record CourseResponse(
        UUID id, String title, String description, Integer durationHours, Integer validityMonths, CourseStatus status) {
}
