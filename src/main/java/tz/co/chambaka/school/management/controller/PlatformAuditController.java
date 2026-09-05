package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.audit.AuditQueryService;
import tz.co.chambaka.school.management.dto.audit.AuditEventResponse;
import tz.co.chambaka.school.management.dto.common.PageResponse;
import tz.co.chambaka.school.management.model.enums.AuditAction;
import tz.co.chambaka.school.management.model.enums.AuditScope;
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
@RequestMapping("/api/v1/platform/audit-events")
@Tag(name = "Platform audit trail")
public class PlatformAuditController {

    private final AuditQueryService auditQueryService;

    public PlatformAuditController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Platform-wide audit trail across all tenants")
    public PageResponse<AuditEventResponse> list(
            @RequestParam(required = false) AuditScope scope,
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) String correctionId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            Pageable pageable
    ) {
        return auditQueryService.search(
                scope, schoolId, correctionId, action, resourceType, actorEmail, from, to, pageable);
    }

    @GetMapping("/{correctionId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "All events for a correctionId across the platform")
    public List<AuditEventResponse> byCorrectionId(@PathVariable String correctionId) {
        return auditQueryService.byCorrectionId(correctionId);
    }
}
