package com.traintrack.coreapi.enrolment.bulk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.traintrack.coreapi.audit.AuditPublisher;
import com.traintrack.coreapi.common.exception.NotFoundException;
import com.traintrack.coreapi.course.CourseRepository;
import com.traintrack.coreapi.domain.BulkEnrolmentJobRow;
import com.traintrack.coreapi.domain.BulkJobRowStatus;
import com.traintrack.coreapi.domain.Course;
import com.traintrack.coreapi.domain.CourseStatus;
import com.traintrack.coreapi.domain.Enrolment;
import com.traintrack.coreapi.domain.EnrolmentStatus;
import com.traintrack.coreapi.domain.Organisation;
import com.traintrack.coreapi.domain.User;
import com.traintrack.coreapi.domain.UserStatus;
import com.traintrack.coreapi.enrolment.EnrolmentRepository;
import com.traintrack.coreapi.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BulkEnrolmentRowProcessorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrolmentRepository enrolmentRepository;

    @Mock
    private BulkEnrolmentJobRowRepository jobRowRepository;

    @Mock
    private AuditPublisher auditPublisher;

    private BulkEnrolmentRowProcessor processor;

    private final UUID orgId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        processor = new BulkEnrolmentRowProcessor(userRepository, courseRepository, enrolmentRepository, jobRowRepository, auditPublisher);
    }

    @Test
    void tryEnrolSavesAnEnrolmentForAValidRow() {
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        User user = new User(new Organisation("Acme"), "e@acme.test", "hash", "Emp", UserStatus.ACTIVE);
        Course course = new Course(new Organisation("Acme"), "Fire Safety", "d", 4, 12, CourseStatus.PUBLISHED);
        Enrolment saved = new Enrolment(orgId, course, user, EnrolmentStatus.ENROLLED, Instant.now());
        when(userRepository.findByIdScoped(userId)).thenReturn(Optional.of(user));
        when(courseRepository.findByIdScoped(courseId)).thenReturn(Optional.of(course));
        when(enrolmentRepository.save(any(Enrolment.class))).thenReturn(saved);

        UUID enrolmentId = processor.tryEnrol(orgId, actorId, new CsvRow(1, userId.toString(), courseId.toString()));

        assertThat(enrolmentId).isEqualTo(saved.getId());
        verify(auditPublisher).record(eq(orgId), eq(actorId), eq("Enrolment"), any(), eq("ENROLMENT_CREATED"), any());
    }

    @Test
    void tryEnrolThrowsForAMalformedUserId() {
        assertThatThrownBy(() -> processor.tryEnrol(orgId, actorId, new CsvRow(1, "not-a-uuid", UUID.randomUUID().toString())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");
    }

    @Test
    void tryEnrolThrowsNotFoundForAnUnknownUser() {
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        when(userRepository.findByIdScoped(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> processor.tryEnrol(orgId, actorId, new CsvRow(1, userId.toString(), courseId.toString())))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void recordSuccessSavesASuccessRow() {
        UUID jobId = UUID.randomUUID();
        UUID enrolmentId = UUID.randomUUID();

        processor.recordSuccess(jobId, new CsvRow(3, "u", "c"), enrolmentId);

        ArgumentCaptor<BulkEnrolmentJobRow> captor = ArgumentCaptor.forClass(BulkEnrolmentJobRow.class);
        verify(jobRowRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(BulkJobRowStatus.SUCCESS);
        assertThat(captor.getValue().getEnrolmentId()).isEqualTo(enrolmentId);
    }

    @Test
    void recordFailureSavesAFailureRowWithTheErrorMessage() {
        UUID jobId = UUID.randomUUID();

        processor.recordFailure(jobId, new CsvRow(3, "u", "c"), "boom");

        ArgumentCaptor<BulkEnrolmentJobRow> captor = ArgumentCaptor.forClass(BulkEnrolmentJobRow.class);
        verify(jobRowRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(BulkJobRowStatus.FAILED);
        assertThat(captor.getValue().getErrorMessage()).isEqualTo("boom");
    }
}
