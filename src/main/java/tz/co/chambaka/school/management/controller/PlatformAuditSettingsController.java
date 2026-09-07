package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.audit.AuditActionSettingsService;
import tz.co.chambaka.school.management.dto.audit.AuditActionSettingResponse;
import tz.co.chambaka.school.management.dto.audit.AuditActionSettingsResponse;
import tz.co.chambaka.school.management.dto.audit.ReplaceAuditActionSettingsRequest;
import tz.co.chambaka.school.management.dto.audit.UpdateAuditActionSettingRequest;
import tz.co.chambaka.school.management.model.enums.AuditAction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform/audit-settings")
@Tag(name = "Platform configurations")
public class PlatformAuditSettingsController {

    private final AuditActionSettingsService settingsService;

    public PlatformAuditSettingsController(AuditActionSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "List auditable events and whether each is enabled")
    public AuditActionSettingsResponse list() {
        return settingsService.list();
    }

    @PutMapping("/{action}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Turn one auditable event on or off")
    public AuditActionSettingResponse update(
            @PathVariable AuditAction action,
            @Valid @RequestBody UpdateAuditActionSettingRequest request
    ) {
        return settingsService.update(action, request.enabled());
    }

    @PutMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Replace enabled flags for one or more auditable events")
    public AuditActionSettingsResponse replaceAll(@Valid @RequestBody ReplaceAuditActionSettingsRequest request) {
        return settingsService.replaceAll(request);
    }
}
