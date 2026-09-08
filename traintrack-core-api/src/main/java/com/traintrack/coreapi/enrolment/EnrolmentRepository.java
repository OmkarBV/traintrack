package com.traintrack.coreapi.enrolment;

import com.traintrack.coreapi.domain.Enrolment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrolmentRepository extends JpaRepository<Enrolment, UUID> {

    @Query("select e from Enrolment e where e.id = :id")
    Optional<Enrolment> findByIdScoped(@Param("id") UUID id);
}
