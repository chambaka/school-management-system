package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.academic.SectionRequest;
import tz.co.chambaka.school.management.dto.academic.SectionResponse;
import tz.co.chambaka.school.management.service.SectionService;
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
@RequestMapping("/api/v1/sections")
@Tag(name = "Sections")
public class SectionController {

    private final SectionService sectionService;
    private final TenantResolver tenantResolver;

    public SectionController(SectionService sectionService, TenantResolver tenantResolver) {
        this.sectionService = sectionService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','TEACHER')")
    public List<SectionResponse> list(@RequestParam Long schoolClassId) {
        return sectionService.list(tenantResolver.requireSchoolId(), schoolClassId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public SectionResponse create(@Valid @RequestBody SectionRequest request) {
        return sectionService.create(tenantResolver.requireSchoolId(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SectionResponse update(@PathVariable Long id, @Valid @RequestBody SectionRequest request) {
        return sectionService.update(tenantResolver.requireSchoolId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        sectionService.delete(tenantResolver.requireSchoolId(), id);
    }
}
