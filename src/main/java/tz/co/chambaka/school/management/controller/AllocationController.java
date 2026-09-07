package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.academic.AllocationRequest;
import tz.co.chambaka.school.management.dto.academic.AllocationResponse;
import tz.co.chambaka.school.management.service.AllocationService;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/allocations")
@Tag(name = "Subject allocation")
public class AllocationController {

    private final AllocationService allocationService;
    private final TenantResolver tenantResolver;

    public AllocationController(AllocationService allocationService, TenantResolver tenantResolver) {
        this.allocationService = allocationService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','TEACHER')")
    public List<AllocationResponse> list(
            @RequestParam(required = false) Long academicYearId,
            @RequestParam(required = false) Long teacherId
    ) {
        return allocationService.list(tenantResolver.requireSchoolId(), academicYearId, teacherId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public AllocationResponse create(@Valid @RequestBody AllocationRequest request) {
        return allocationService.create(tenantResolver.requireSchoolId(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        allocationService.delete(tenantResolver.requireSchoolId(), id);
    }
}
