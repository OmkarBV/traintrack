package com.traintrack.auditservice.audit;

import com.traintrack.auditservice.audit.dto.AuditEventResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditController {

    private final AuditQueryService auditQueryService;
    private final AuditEventMapper auditEventMapper;

    public AuditController(AuditQueryService auditQueryService, AuditEventMapper auditEventMapper) {
        this.auditQueryService = auditQueryService;
        this.auditEventMapper = auditEventMapper;
    }

    @GetMapping("/api/v1/audit")
    public Page<AuditEventResponse> search(
            @RequestParam(required = false) UUID orgId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {
        return auditQueryService.search(orgId, entityType, from, to, pageable).map(auditEventMapper::toResponse);
    }
}
