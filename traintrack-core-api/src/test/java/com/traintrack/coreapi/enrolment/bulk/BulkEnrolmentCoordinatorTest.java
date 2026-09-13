package com.traintrack.coreapi.enrolment.bulk;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.traintrack.coreapi.domain.BulkEnrolmentJob;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BulkEnrolmentCoordinatorTest {

    @Mock
    private BulkEnrolmentRowProcessor rowProcessor;

    @Mock
    private BulkEnrolmentJobRepository jobRepository;

    private BulkEnrolmentCoordinator coordinator;

    @BeforeEach
    void setUp() {
        coordinator = new BulkEnrolmentCoordinator(rowProcessor, jobRepository, Runnable::run);
    }

    @Test
    void jobStillCompletesWhenRecordingAFailureItselfThrows() {
        UUID jobId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        BulkEnrolmentJob job = mock(BulkEnrolmentJob.class);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        CsvRow row = new CsvRow(1, "bad-user-id", "bad-course-id");
        when(rowProcessor.tryEnrol(orgId, actorId, row)).thenThrow(new IllegalArgumentException("Invalid userId"));
        doThrow(new RuntimeException("db down")).when(rowProcessor).recordFailure(any(), any(), any());

        coordinator.processAsync(jobId, orgId, actorId, List.of(row));

        verify(job).markCompleted();
    }
}
