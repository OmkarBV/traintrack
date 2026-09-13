package com.traintrack.coreapi.enrolment.bulk;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Fans a job out across the bounded worker pool and marks the job COMPLETED
 * once every row has resolved, one way or the other. Runs on the virtual-
 * thread coordinator executor (see BulkEnrolmentExecutorConfig) so that
 * {@link #processAsync} — invoked via {@code @Async} from
 * {@code BulkEnrolmentService.submit}, which must return immediately with a
 * 202 — does not tie up one of the bounded pool's own threads just to sit
 * blocked on {@code CompletableFuture.allOf(...).join()}.
 */
@Component
public class BulkEnrolmentCoordinator {

    private static final Logger log = LoggerFactory.getLogger(BulkEnrolmentCoordinator.class);

    private final BulkEnrolmentRowProcessor rowProcessor;
    private final BulkEnrolmentJobRepository jobRepository;
    private final Executor bulkEnrolmentExecutor;

    public BulkEnrolmentCoordinator(
            BulkEnrolmentRowProcessor rowProcessor,
            BulkEnrolmentJobRepository jobRepository,
            @Qualifier("bulkEnrolmentExecutor") Executor bulkEnrolmentExecutor) {
        this.rowProcessor = rowProcessor;
        this.jobRepository = jobRepository;
        this.bulkEnrolmentExecutor = bulkEnrolmentExecutor;
    }

    @Async("bulkCoordinatorExecutor")
    public void processAsync(UUID jobId, UUID orgId, UUID actorId, List<CsvRow> rows) {
        markProcessing(jobId);

        List<CompletableFuture<Void>> futures = rows.stream()
                .map(row -> CompletableFuture.runAsync(() -> processOneRow(jobId, orgId, actorId, row), bulkEnrolmentExecutor))
                .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        markCompleted(jobId);
    }

    private void processOneRow(UUID jobId, UUID orgId, UUID actorId, CsvRow row) {
        try {
            UUID enrolmentId = rowProcessor.tryEnrol(orgId, actorId, row);
            rowProcessor.recordSuccess(jobId, row, enrolmentId);
        } catch (Exception e) {
            log.debug("Bulk enrolment row {} of job {} failed: {}", row.rowNumber(), jobId, e.getMessage());
            recordFailureSafely(jobId, row, e.getMessage());
        }
    }

    private void recordFailureSafely(UUID jobId, CsvRow row, String errorMessage) {
        try {
            rowProcessor.recordFailure(jobId, row, errorMessage);
        } catch (Exception e) {
            log.error("Bulk enrolment row {} of job {} failed, and recording that failure also failed", row.rowNumber(), jobId, e);
        }
    }

    private void markProcessing(UUID jobId) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.markProcessing();
            jobRepository.save(job);
        });
    }

    private void markCompleted(UUID jobId) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.markCompleted();
            jobRepository.save(job);
        });
    }
}
