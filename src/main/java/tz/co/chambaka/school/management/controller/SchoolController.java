package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.school.SchoolResponse;
import tz.co.chambaka.school.management.dto.school.UpdateSchoolRequest;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.SchoolService;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Schools")
public class SchoolController {

    private final SchoolService schoolService;
    private final TenantResolver tenantResolver;

    public SchoolController(SchoolService schoolService, TenantResolver tenantResolver) {
        this.schoolService = schoolService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping("/platform/schools")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<SchoolResponse> listAll() {
        return schoolService.list();
    }

    @GetMapping("/schools/current")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','TEACHER','STUDENT','PARENT')")
    public SchoolResponse current(@CurrentUser UserPrincipal principal) {
        return schoolService.get(tenantResolver.requireSchoolId());
    }

    @PutMapping("/schools/current")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    public SchoolResponse updateCurrent(@Valid @RequestBody UpdateSchoolRequest request) {
        return schoolService.update(tenantResolver.requireSchoolId(), request);
    }

    @PutMapping("/platform/schools/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public SchoolResponse updatePlatform(@PathVariable Long id, @Valid @RequestBody UpdateSchoolRequest request) {
        return schoolService.update(id, request);
    }
}
