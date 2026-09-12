package com.traintrack.coreapi.certification;

import com.traintrack.coreapi.domain.Certification;
import com.traintrack.coreapi.domain.CertificationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CertificationRepository
        extends JpaRepository<Certification, UUID>, JpaSpecificationExecutor<Certification> {

    Page<Certification> findByUserId(UUID userId, Pageable pageable);

    /** See UserRepository.findByIdScoped for why this goes through JPQL rather than findById. */
    @Query("select c from Certification c where c.id = :id")
    Optional<Certification> findByIdScoped(@Param("id") UUID id);

    /**
     * Runs without an org context (no authenticated caller for a background
     * job), so the tenant-scoping filter is naturally off here — correct,
     * since a daily job must see every organisation's certifications, not
     * just one.
     */
    @Query("select c from Certification c where c.status = :status and c.expiresAt < :now")
    List<Certification> findLapsed(@Param("status") CertificationStatus status, @Param("now") Instant now);

    @Query(
            "select c from Certification c where c.status = :status and c.expiresAt between :now and :windowEnd "
                    + "and c.expiringNotifiedAt is null")
    List<Certification> findExpiringSoonUnnotified(
            @Param("status") CertificationStatus status, @Param("now") Instant now, @Param("windowEnd") Instant windowEnd);
}
