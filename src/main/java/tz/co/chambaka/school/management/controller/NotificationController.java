package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.notification.NotificationResponse;
import tz.co.chambaka.school.management.security.Access;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.AlertService;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications")
public class NotificationController {

    private final AlertService alertService;
    private final TenantResolver tenantResolver;

    public NotificationController(AlertService alertService, TenantResolver tenantResolver) {
        this.alertService = alertService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize(Access.SCHOOL_USER)
    public List<NotificationResponse> inbox(@CurrentUser UserPrincipal principal) {
        return alertService.inbox(tenantResolver.requireSchoolId(), principal.getId());
    }

    @PostMapping("/{id}/read")
    @PreAuthorize(Access.SCHOOL_USER)
    public void markRead(@CurrentUser UserPrincipal principal, @PathVariable Long id) {
        alertService.markRead(principal.getId(), id);
    }
}
