package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.audit.AuditQueryService;
import tz.co.chambaka.school.management.dto.audit.AuditEventResponse;
import tz.co.chambaka.school.management.dto.common.PageResponse;
import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.model.enums.AuditScope;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-events")
@Tag(name = "Audit trail")
public class AuditEventController {

    private final AuditQueryService auditQueryService;
    private final TenantResolver tenantResolver;

    public AuditEventController(AuditQueryService auditQueryService, TenantResolver tenantResolver) {
        this.auditQueryService = auditQueryService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    @Operation(summary = "Tenant audit trail for the current school")
    public PageResponse<AuditEventResponse> list(
            @RequestParam(required = false) String correctionId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            Pageable pageable
    ) {
        return auditQueryService.search(
                null,
                tenantResolver.requireSchoolId(),
                correctionId,
                action,
                resourceType,
                actorEmail,
                from,
                to,
                pageable
        );
    }

    @GetMapping("/{correctionId}")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    @Operation(summary = "Tenant events sharing a correctionId")
    public List<AuditEventResponse> byCorrectionId(@PathVariable String correctionId) {
        return auditQueryService.byCorrectionIdForSchool(correctionId, tenantResolver.requireSchoolId());
    }
}
