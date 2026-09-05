package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.campus.CampusResponse;
import tz.co.chambaka.school.management.dto.campus.CreateCampusRequest;
import tz.co.chambaka.school.management.dto.campus.UpdateCampusRequest;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.CampusService;
import tz.co.chambaka.school.management.service.TenantService;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/campuses")
@Tag(name = "Campuses")
public class CampusController {

    private final CampusService campusService;
    private final TenantService tenantService;
    private final TenantResolver tenantResolver;

    public CampusController(CampusService campusService, TenantService tenantService, TenantResolver tenantResolver) {
        this.campusService = campusService;
        this.tenantService = tenantService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','TEACHER')")
    public List<CampusResponse> list(
            @RequestParam(required = false) Long schoolId,
            @CurrentUser UserPrincipal principal
    ) {
        return campusService.listBySchool(resolveSchool(schoolId, principal));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CampusResponse create(
            @RequestParam(required = false) Long schoolId,
            @Valid @RequestBody CreateCampusRequest request,
            @CurrentUser UserPrincipal principal
    ) {
        return campusService.create(resolveSchool(schoolId, principal), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    public CampusResponse update(
            @PathVariable Long id,
            @RequestParam(required = false) Long schoolId,
            @Valid @RequestBody UpdateCampusRequest request,
            @CurrentUser UserPrincipal principal
    ) {
        return campusService.update(resolveSchool(schoolId, principal), id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @RequestParam(required = false) Long schoolId,
            @CurrentUser UserPrincipal principal
    ) {
        campusService.delete(resolveSchool(schoolId, principal), id);
    }

    private Long resolveSchool(Long requestedSchoolId, UserPrincipal principal) {
        Long schoolId = tenantResolver.resolve(requestedSchoolId, principal);
        if (principal.getTenantId() != null) {
            tenantService.requireSchoolInTenant(principal.getTenantId(), schoolId);
        }
        return schoolId;
    }
}
