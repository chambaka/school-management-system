package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.academic.SchoolClassRequest;
import tz.co.chambaka.school.management.dto.academic.SchoolClassResponse;
import tz.co.chambaka.school.management.service.ClassService;
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
@RequestMapping("/api/v1/classes")
@Tag(name = "Classes")
public class ClassController {

    private final ClassService classService;
    private final TenantResolver tenantResolver;

    public ClassController(ClassService classService, TenantResolver tenantResolver) {
        this.classService = classService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','TEACHER')")
    public List<SchoolClassResponse> list(@RequestParam(required = false) Long academicYearId) {
        return classService.list(tenantResolver.requireSchoolId(), academicYearId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public SchoolClassResponse create(@Valid @RequestBody SchoolClassRequest request) {
        return classService.create(tenantResolver.requireSchoolId(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SchoolClassResponse update(@PathVariable Long id, @Valid @RequestBody SchoolClassRequest request) {
        return classService.update(tenantResolver.requireSchoolId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        classService.delete(tenantResolver.requireSchoolId(), id);
    }
}
