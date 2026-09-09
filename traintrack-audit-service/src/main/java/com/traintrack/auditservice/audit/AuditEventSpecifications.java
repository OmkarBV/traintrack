package com.traintrack.auditservice.audit;

import com.traintrack.auditservice.domain.AuditEventRecord;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/** See CertificationSpecifications in core-api for why this pattern (rather than JPQL "is null or") is used. */
final class AuditEventSpecifications {

    private AuditEventSpecifications() {
    }

    static Specification<AuditEventRecord> hasOrgId(UUID orgId) {
        return (root, query, cb) -> orgId == null ? null : cb.equal(root.get("orgId"), orgId);
    }

    static Specification<AuditEventRecord> hasEntityType(String entityType) {
        return (root, query, cb) -> entityType == null ? null : cb.equal(root.get("entityType"), entityType);
    }

    static Specification<AuditEventRecord> occurredFrom(Instant from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("occurredAt"), from);
    }

    static Specification<AuditEventRecord> occurredTo(Instant to) {
        return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("occurredAt"), to);
    }
}
