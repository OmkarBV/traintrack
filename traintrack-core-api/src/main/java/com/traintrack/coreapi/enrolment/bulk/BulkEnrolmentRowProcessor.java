package com.traintrack.coreapi.enrolment.bulk;

import com.traintrack.coreapi.audit.AuditPublisher;
import com.traintrack.coreapi.common.exception.NotFoundException;
import com.traintrack.coreapi.course.CourseRepository;
import com.traintrack.coreapi.domain.BulkEnrolmentJobRow;
import com.traintrack.coreapi.domain.Course;
import com.traintrack.coreapi.domain.Enrolment;
import com.traintrack.coreapi.domain.EnrolmentStatus;
import com.traintrack.coreapi.domain.User;
import com.traintrack.coreapi.enrolment.EnrolmentRepository;
import com.traintrack.coreapi.user.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Each method here is its own independent transaction — required both for
 * "each row commits independently" and because a Postgres transaction that
 * hits an error can't run further statements until rolled back: attempting
 * the enrolment and recording its outcome cannot be one transaction, or a
 * failed enrolment attempt would poison the very transaction trying to
 * record that failure. See {@code BulkEnrolmentCoordinator}, which calls
 * these three methods through this bean's proxy (not from within this same
 * class), which is what makes each {@code @Transactional} actually take
 * effect rather than being silently skipped by Spring AOP's self-invocation
 * limitation.
 */
@Component
public class BulkEnrolmentRowProcessor {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final BulkEnrolmentJobRowRepository jobRowRepository;
    private final AuditPublisher auditPublisher;

    public BulkEnrolmentRowProcessor(
            UserRepository userRepository,
            CourseRepository courseRepository,
            EnrolmentRepository enrolmentRepository,
            BulkEnrolmentJobRowRepository jobRowRepository,
            AuditPublisher auditPublisher) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrolmentRepository = enrolmentRepository;
        this.jobRowRepository = jobRowRepository;
        this.auditPublisher = auditPublisher;
    }

    @Transactional
    public UUID tryEnrol(UUID orgId, UUID actorId, CsvRow row) {
        UUID userId = parseUuid(row.rawUserId(), "userId");
        UUID courseId = parseUuid(row.rawCourseId(), "courseId");
        User user = userRepository.findByIdScoped(userId).orElseThrow(() -> new NotFoundException("User not found: " + userId));
        Course course =
                courseRepository.findByIdScoped(courseId).orElseThrow(() -> new NotFoundException("Course not found: " + courseId));
        Enrolment enrolment = enrolmentRepository.save(new Enrolment(orgId, course, user, EnrolmentStatus.ENROLLED, Instant.now()));
        auditPublisher.record(
                orgId,
                actorId,
                "Enrolment",
                enrolment.getId(),
                "ENROLMENT_CREATED",
                "Bulk-enrolled user '" + user.getEmail() + "' in course '" + course.getTitle() + "'");
        return enrolment.getId();
    }

    @Transactional
    public void recordSuccess(UUID jobId, CsvRow row, UUID enrolmentId) {
        jobRowRepository.save(
                BulkEnrolmentJobRow.success(jobId, row.rowNumber(), row.rawUserId(), row.rawCourseId(), enrolmentId));
    }

    @Transactional
    public void recordFailure(UUID jobId, CsvRow row, String errorMessage) {
        jobRowRepository.save(
                BulkEnrolmentJobRow.failure(jobId, row.rowNumber(), row.rawUserId(), row.rawCourseId(), errorMessage));
    }

    private UUID parseUuid(String raw, String fieldName) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid " + fieldName + ": '" + raw + "'");
        }
    }
}
