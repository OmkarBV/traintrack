package com.traintrack.auditservice.audit;

import com.traintrack.auditservice.audit.dto.AuditEventResponse;
import com.traintrack.auditservice.domain.AuditEventRecord;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditEventMapper {

    AuditEventResponse toResponse(AuditEventRecord record);
}
