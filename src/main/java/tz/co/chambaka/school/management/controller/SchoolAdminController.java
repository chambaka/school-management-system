package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.admin.CreateSchoolAdminRequest;
import tz.co.chambaka.school.management.dto.admin.SchoolAdminResponse;
import tz.co.chambaka.school.management.dto.admin.UpdateSchoolAdminRequest;
import tz.co.chambaka.school.management.dto.common.PageResponse;
import tz.co.chambaka.school.management.service.SchoolAdminService;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
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

@RestController
@RequestMapping("/api/v1/school-admins")
@Tag(name = "School admins")
public class SchoolAdminController {

    private final SchoolAdminService schoolAdminService;
    private final TenantResolver tenantResolver;

    public SchoolAdminController(SchoolAdminService schoolAdminService, TenantResolver tenantResolver) {
        this.schoolAdminService = schoolAdminService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','SUPER_ADMIN')")
    public PageResponse<SchoolAdminResponse> list(Pageable pageable) {
        return schoolAdminService.list(tenantResolver.requireSchoolId(), pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','SUPER_ADMIN')")
    public SchoolAdminResponse get(@PathVariable Long id) {
        return schoolAdminService.get(tenantResolver.requireSchoolId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','SUPER_ADMIN')")
    public SchoolAdminResponse create(@Valid @RequestBody CreateSchoolAdminRequest request) {
        return schoolAdminService.create(tenantResolver.requireSchoolId(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','SUPER_ADMIN')")
    public SchoolAdminResponse update(@PathVariable Long id, @Valid @RequestBody UpdateSchoolAdminRequest request) {
        return schoolAdminService.update(tenantResolver.requireSchoolId(), id, request);
    }
}
