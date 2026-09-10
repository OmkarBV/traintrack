package com.traintrack.coreapi.enrolment.bulk;

import com.traintrack.coreapi.domain.BulkEnrolmentJob;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BulkEnrolmentJobRepository extends JpaRepository<BulkEnrolmentJob, UUID> {

    @Query("select j from BulkEnrolmentJob j where j.id = :id")
    Optional<BulkEnrolmentJob> findByIdScoped(@Param("id") UUID id);
}
