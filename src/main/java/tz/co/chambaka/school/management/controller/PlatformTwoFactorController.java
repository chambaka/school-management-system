package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.auth.SchoolTwoFactorResponse;
import tz.co.chambaka.school.management.dto.auth.TwoFactorSettingsResponse;
import tz.co.chambaka.school.management.dto.auth.UpdateTwoFactorSettingsRequest;
import tz.co.chambaka.school.management.service.TwoFactorService;
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
@RequestMapping("/api/v1/platform/two-factor")
@Tag(name = "Platform configurations")
public class PlatformTwoFactorController {

    private final TwoFactorService twoFactorService;

    public PlatformTwoFactorController(TwoFactorService twoFactorService) {
        this.twoFactorService = twoFactorService;
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "ShuleHub 2FA on/off for each school")
    public TwoFactorSettingsResponse get() {
        return twoFactorService.settings();
    }

    @PutMapping("/{schoolId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Turn ShuleHub 2FA on or off for one school. Platform admin only.")
    public SchoolTwoFactorResponse update(
            @PathVariable Long schoolId,
            @Valid @RequestBody UpdateTwoFactorSettingsRequest request
    ) {
        return twoFactorService.updateEnabled(schoolId, request.enabled());
    }
}
