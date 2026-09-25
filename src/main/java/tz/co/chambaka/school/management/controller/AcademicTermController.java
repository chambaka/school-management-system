package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.academic.AcademicTermRequest;
import tz.co.chambaka.school.management.dto.academic.AcademicTermResponse;
import tz.co.chambaka.school.management.security.Access;
import tz.co.chambaka.school.management.service.AcademicTermService;
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
@RequestMapping("/api/v1/academic-terms")
@Tag(name = "Academic terms")
public class AcademicTermController {

    private final AcademicTermService academicTermService;
    private final TenantResolver tenantResolver;

    public AcademicTermController(AcademicTermService academicTermService, TenantResolver tenantResolver) {
        this.academicTermService = academicTermService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize(Access.ACADEMIC_STAFF)
    public List<AcademicTermResponse> list(@RequestParam(required = false) Long academicYearId) {
        return academicTermService.list(tenantResolver.requireSchoolId(), academicYearId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.PEOPLE_MANAGE)
    public AcademicTermResponse create(@Valid @RequestBody AcademicTermRequest request) {
        return academicTermService.create(tenantResolver.requireSchoolId(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.PEOPLE_MANAGE)
    public void delete(@PathVariable Long id) {
        academicTermService.delete(tenantResolver.requireSchoolId(), id);
    }
}
