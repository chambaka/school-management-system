package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.academic.DepartmentRequest;
import tz.co.chambaka.school.management.dto.academic.DepartmentResponse;
import tz.co.chambaka.school.management.service.DepartmentService;
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
@RequestMapping("/api/v1/departments")
@Tag(name = "Departments")
public class DepartmentController {

    private final DepartmentService departmentService;
    private final TenantResolver tenantResolver;

    public DepartmentController(DepartmentService departmentService, TenantResolver tenantResolver) {
        this.departmentService = departmentService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','TEACHER')")
    public List<DepartmentResponse> list() {
        return departmentService.list(tenantResolver.requireSchoolId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentResponse create(@Valid @RequestBody DepartmentRequest request) {
        return departmentService.create(tenantResolver.requireSchoolId(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentResponse update(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        return departmentService.update(tenantResolver.requireSchoolId(), id, request);
    }
}
