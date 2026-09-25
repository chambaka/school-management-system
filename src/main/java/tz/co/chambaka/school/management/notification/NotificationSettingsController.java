package tz.co.chambaka.school.management.notification;

import tz.co.chambaka.school.management.security.Access;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notification-settings")
@Tag(name = "Notification settings")
public class NotificationSettingsController {

    private final NotificationSettingsService settingsService;
    private final TenantResolver tenantResolver;

    public NotificationSettingsController(NotificationSettingsService settingsService, TenantResolver tenantResolver) {
        this.settingsService = settingsService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping("/templates")
    @PreAuthorize(Access.NOTICE_WRITE)
    public List<NotificationTemplateResponse> templates() {
        return settingsService.templates(tenantResolver.requireSchoolId());
    }

    @PostMapping("/templates")
    @PreAuthorize(Access.NOTICE_WRITE)
    public NotificationTemplateResponse saveTemplate(@Valid @RequestBody NotificationTemplateRequest request) {
        return settingsService.saveTemplate(tenantResolver.requireSchoolId(), request);
    }

    @GetMapping("/preferences")
    @PreAuthorize(Access.SCHOOL_USER)
    public List<NotificationPreferenceResponse> preferences(@CurrentUser UserPrincipal principal) {
        return settingsService.preferences(principal.getId());
    }

    @PostMapping("/preferences")
    @PreAuthorize(Access.SCHOOL_USER)
    public NotificationPreferenceResponse savePreference(
            @CurrentUser UserPrincipal principal,
            @Valid @RequestBody NotificationPreferenceRequest request
    ) {
        return settingsService.savePreference(principal.getId(), request);
    }
}
