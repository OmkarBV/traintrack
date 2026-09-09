package com.traintrack.auditservice.audit;

import com.traintrack.auditservice.domain.AuditEventRecord;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditQueryService {

    private final AuditEventRepository auditEventRepository;

    public AuditQueryService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional(readOnly = true)
    public Page<AuditEventRecord> search(UUID orgId, String entityType, Instant from, Instant to, Pageable pageable) {
        Specification<AuditEventRecord> spec = Specification.where(AuditEventSpecifications.hasOrgId(orgId))
                .and(AuditEventSpecifications.hasEntityType(entityType))
                .and(AuditEventSpecifications.occurredFrom(from))
                .and(AuditEventSpecifications.occurredTo(to));
        return auditEventRepository.findAll(spec, pageable);
    }
}
