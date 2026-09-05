package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.campus.CampusResponse;
import tz.co.chambaka.school.management.dto.school.CreateSchoolRequest;
import tz.co.chambaka.school.management.dto.school.SchoolResponse;
import tz.co.chambaka.school.management.dto.tenant.CreateTenantRequest;
import tz.co.chambaka.school.management.dto.tenant.TenantResponse;
import tz.co.chambaka.school.management.dto.tenant.UpdateTenantRequest;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.CampusService;
import tz.co.chambaka.school.management.service.TenantService;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Tenants")
public class TenantController {

    private final TenantService tenantService;
    private final CampusService campusService;
    private final TenantResolver tenantResolver;

    public TenantController(TenantService tenantService, CampusService campusService, TenantResolver tenantResolver) {
        this.tenantService = tenantService;
        this.campusService = campusService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping("/platform/tenants")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<TenantResponse> listAll() {
        return tenantService.list();
    }

    @PostMapping("/platform/tenants")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse create(@Valid @RequestBody CreateTenantRequest request) {
        return tenantService.create(request);
    }

    @GetMapping("/platform/tenants/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public TenantResponse get(@PathVariable Long id) {
        return tenantService.get(id);
    }

    @PutMapping("/platform/tenants/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public TenantResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTenantRequest request) {
        return tenantService.update(id, request);
    }

    @GetMapping("/platform/tenants/{id}/schools")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<SchoolResponse> platformSchools(@PathVariable Long id) {
        return tenantService.listSchools(id);
    }

    @PostMapping("/platform/tenants/{id}/schools")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolResponse platformAddSchool(@PathVariable Long id, @Valid @RequestBody CreateSchoolRequest request) {
        return tenantService.addSchool(id, request);
    }

    @GetMapping("/tenants/current")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public TenantResponse current() {
        return tenantService.get(tenantResolver.requireTenantId());
    }

    @GetMapping("/tenants/current/schools")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public List<SchoolResponse> currentSchools() {
        return tenantService.listSchools(tenantResolver.requireTenantId());
    }

    @PostMapping("/tenants/current/schools")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolResponse addSchool(@Valid @RequestBody CreateSchoolRequest request) {
        return tenantService.addSchool(tenantResolver.requireTenantId(), request);
    }

    @GetMapping("/tenants/current/campuses")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public List<CampusResponse> currentCampuses() {
        return campusService.listByTenant(tenantResolver.requireTenantId());
    }

    @GetMapping("/tenants/current/schools/{schoolId}/campuses")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public List<CampusResponse> schoolCampuses(
            @PathVariable Long schoolId,
            @CurrentUser UserPrincipal principal
    ) {
        tenantService.requireSchoolInTenant(principal.getTenantId(), schoolId);
        return campusService.listBySchool(schoolId);
    }
}
