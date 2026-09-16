package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.dashboard.DashboardResponse;
import tz.co.chambaka.school.management.security.Access;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.DashboardService;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final TenantResolver tenantResolver;

    public DashboardController(DashboardService dashboardService, TenantResolver tenantResolver) {
        this.dashboardService = dashboardService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize(Access.SCHOOL_USER)
    public DashboardResponse snapshot(@CurrentUser UserPrincipal principal) {
        return dashboardService.snapshot(tenantResolver.requireSchoolId(), principal.getRole());
    }
}
