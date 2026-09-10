package com.traintrack.coreapi.enrolment.bulk;

import com.traintrack.coreapi.common.exception.BadRequestException;
import com.traintrack.coreapi.common.exception.NotFoundException;
import com.traintrack.coreapi.domain.BulkEnrolmentJob;
import com.traintrack.coreapi.domain.BulkEnrolmentJobRow;
import com.traintrack.coreapi.domain.BulkJobRowStatus;
import com.traintrack.coreapi.enrolment.bulk.dto.BulkEnrolmentJobResponse;
import com.traintrack.coreapi.enrolment.bulk.dto.BulkEnrolmentRowResult;
import com.traintrack.coreapi.security.CurrentUser;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BulkEnrolmentService {

    private final BulkEnrolmentJobRepository jobRepository;
    private final BulkEnrolmentJobRowRepository jobRowRepository;
    private final BulkEnrolmentCoordinator coordinator;

    public BulkEnrolmentService(
            BulkEnrolmentJobRepository jobRepository,
            BulkEnrolmentJobRowRepository jobRowRepository,
            BulkEnrolmentCoordinator coordinator) {
        this.jobRepository = jobRepository;
        this.jobRowRepository = jobRowRepository;
        this.coordinator = coordinator;
    }

    @Transactional
    public BulkEnrolmentJobResponse submit(MultipartFile file) {
        List<CsvRow> rows = CsvParser.parse(uncheckedInputStream(file));
        UUID orgId = CurrentUser.requireOrgId();
        UUID actorId = CurrentUser.requireUserId();

        BulkEnrolmentJob job = new BulkEnrolmentJob(orgId, rows.size(), actorId);
        jobRepository.save(job);

        coordinator.processAsync(job.getId(), orgId, actorId, rows);

        return toResponse(job, List.of());
    }

    @Transactional(readOnly = true)
    public BulkEnrolmentJobResponse getStatus(UUID jobId) {
        BulkEnrolmentJob job = jobRepository
                .findByIdScoped(jobId)
                .orElseThrow(() -> new NotFoundException("Bulk enrolment job not found: " + jobId));
        List<BulkEnrolmentJobRow> rows = jobRowRepository.findAllForJob(jobId);
        return toResponse(job, rows);
    }

    private BulkEnrolmentJobResponse toResponse(BulkEnrolmentJob job, List<BulkEnrolmentJobRow> rows) {
        long succeeded = rows.stream().filter(r -> r.getStatus() == BulkJobRowStatus.SUCCESS).count();
        long failed = rows.stream().filter(r -> r.getStatus() == BulkJobRowStatus.FAILED).count();
        List<BulkEnrolmentRowResult> rowResults = rows.stream()
                .map(r -> new BulkEnrolmentRowResult(
                        r.getId().rowNumber(), r.getRawUserId(), r.getRawCourseId(), r.getStatus(), r.getEnrolmentId(), r.getErrorMessage()))
                .toList();
        return new BulkEnrolmentJobResponse(
                job.getId(), job.getStatus(), job.getTotalRows(), succeeded, failed, job.getCreatedAt(), job.getCompletedAt(), rowResults);
    }

    private InputStream uncheckedInputStream(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (IOException e) {
            throw new BadRequestException("Failed to read the uploaded file");
        }
    }
}
