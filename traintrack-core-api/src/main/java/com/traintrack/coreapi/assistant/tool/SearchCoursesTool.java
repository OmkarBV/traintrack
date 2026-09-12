package com.traintrack.coreapi.assistant.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.traintrack.coreapi.assistant.AssistantTool;
import com.traintrack.coreapi.course.CourseMapper;
import com.traintrack.coreapi.course.CourseService;
import com.traintrack.coreapi.course.dto.CourseResponse;
import com.traintrack.coreapi.security.CurrentUser;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class SearchCoursesTool implements AssistantTool {

    private final CourseService courseService;
    private final CourseMapper courseMapper;

    public SearchCoursesTool(CourseService courseService, CourseMapper courseMapper) {
        this.courseService = courseService;
        this.courseMapper = courseMapper;
    }

    @Override
    public String name() {
        return "search_courses";
    }

    @Override
    public String description() {
        return "Lists the organisation's course catalog: title, description, duration, validity period, and status.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(), "required", List.of());
    }

    @Override
    public Object execute(JsonNode input) {
        var caller = CurrentUser.get().orElseThrow(() -> new AccessDeniedException("No authenticated user"));
        if (!caller.permissions().contains("COURSE_VIEW")) {
            throw new AccessDeniedException("Missing COURSE_VIEW permission");
        }

        List<CourseResponse> courses =
                courseService.list(PageRequest.of(0, 50)).map(courseMapper::toResponse).getContent();
        return Map.of("courses", courses);
    }
}
