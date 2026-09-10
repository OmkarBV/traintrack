package com.traintrack.coreapi.enrolment.bulk;

import com.traintrack.coreapi.domain.BulkEnrolmentJobRow;
import com.traintrack.coreapi.domain.BulkEnrolmentJobRowId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BulkEnrolmentJobRowRepository extends JpaRepository<BulkEnrolmentJobRow, BulkEnrolmentJobRowId> {

    @Query("select r from BulkEnrolmentJobRow r where r.id.jobId = :jobId order by r.id.rowNumber asc")
    List<BulkEnrolmentJobRow> findAllForJob(@Param("jobId") UUID jobId);
}
