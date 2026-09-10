package com.traintrack.coreapi.enrolment.bulk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.traintrack.coreapi.common.exception.NotFoundException;
import com.traintrack.coreapi.domain.BulkEnrolmentJob;
import com.traintrack.coreapi.domain.BulkEnrolmentJobRow;
import com.traintrack.coreapi.domain.BulkJobStatus;
import com.traintrack.coreapi.enrolment.bulk.dto.BulkEnrolmentJobResponse;
import com.traintrack.coreapi.security.AuthenticatedUser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class BulkEnrolmentServiceTest {

    @Mock
    private BulkEnrolmentJobRepository jobRepository;

    @Mock
    private BulkEnrolmentJobRowRepository jobRowRepository;

    @Mock
    private BulkEnrolmentCoordinator coordinator;

    private BulkEnrolmentService service;
    private final UUID orgId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new BulkEnrolmentService(jobRepository, jobRowRepository, coordinator);
        AuthenticatedUser principal = new AuthenticatedUser(userId, orgId, "admin@acme.test", Set.of("ENROLMENT_CREATE"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitCreatesAPendingJobAndKicksOffAsyncProcessing() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "bulk.csv", "text/csv",
                "userId,courseId\nu1,c1\nu2,c2\n".getBytes(StandardCharsets.UTF_8));

        BulkEnrolmentJobResponse response = service.submit(file);

        assertThat(response.status()).isEqualTo(BulkJobStatus.PENDING);
        assertThat(response.totalRows()).isEqualTo(2);
        assertThat(response.rows()).isEmpty();

        ArgumentCaptor<BulkEnrolmentJob> jobCaptor = ArgumentCaptor.forClass(BulkEnrolmentJob.class);
        verify(jobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getOrgId()).isEqualTo(orgId);
        assertThat(jobCaptor.getValue().getTotalRows()).isEqualTo(2);

        verify(coordinator).processAsync(eq(jobCaptor.getValue().getId()), eq(orgId), eq(userId), any(List.class));
    }

    @Test
    void getStatusThrowsNotFoundForAMissingOrForeignOrgJob() {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findByIdScoped(jobId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStatus(jobId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getStatusAggregatesSucceededAndFailedCounts() {
        UUID jobId = UUID.randomUUID();
        BulkEnrolmentJob job = new BulkEnrolmentJob(orgId, 3, userId);
        job.markCompleted();
        when(jobRepository.findByIdScoped(jobId)).thenReturn(Optional.of(job));
        when(jobRowRepository.findAllForJob(jobId))
                .thenReturn(List.of(
                        BulkEnrolmentJobRow.success(jobId, 1, "u1", "c1", UUID.randomUUID()),
                        BulkEnrolmentJobRow.success(jobId, 2, "u2", "c1", UUID.randomUUID()),
                        BulkEnrolmentJobRow.failure(jobId, 3, "bad", "c1", "User not found: bad")));

        BulkEnrolmentJobResponse response = service.getStatus(jobId);

        assertThat(response.succeededRows()).isEqualTo(2);
        assertThat(response.failedRows()).isEqualTo(1);
        assertThat(response.rows()).hasSize(3);
    }
}
