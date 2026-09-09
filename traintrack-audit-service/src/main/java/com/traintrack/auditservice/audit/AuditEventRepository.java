package com.traintrack.auditservice.audit;

import com.traintrack.auditservice.domain.AuditEventRecord;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditEventRepository extends JpaRepository<AuditEventRecord, UUID>, JpaSpecificationExecutor<AuditEventRecord> {
}
