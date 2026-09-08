package com.traintrack.coreapi.certification;

import com.traintrack.coreapi.domain.Certification;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CertificationRepository
        extends JpaRepository<Certification, UUID>, JpaSpecificationExecutor<Certification> {

    Page<Certification> findByUserId(UUID userId, Pageable pageable);
}
