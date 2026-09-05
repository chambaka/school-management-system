package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.academic.AcademicYearRequest;
import tz.co.chambaka.school.management.dto.academic.AcademicYearResponse;
import tz.co.chambaka.school.management.service.AcademicYearService;
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
@RequestMapping("/api/v1/academic-years")
@Tag(name = "Academic years")
public class AcademicYearController {

    private final AcademicYearService academicYearService;
    private final TenantResolver tenantResolver;

    public AcademicYearController(AcademicYearService academicYearService, TenantResolver tenantResolver) {
        this.academicYearService = academicYearService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','TEACHER')")
    public List<AcademicYearResponse> list() {
        return academicYearService.list(tenantResolver.requireSchoolId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    public AcademicYearResponse create(@Valid @RequestBody AcademicYearRequest request) {
        return academicYearService.create(tenantResolver.requireSchoolId(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    public AcademicYearResponse update(@PathVariable Long id, @Valid @RequestBody AcademicYearRequest request) {
        return academicYearService.update(tenantResolver.requireSchoolId(), id, request);
    }

    @PostMapping("/{id}/current")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    public void setCurrent(@PathVariable Long id) {
        academicYearService.setCurrent(tenantResolver.requireSchoolId(), id);
    }
}
