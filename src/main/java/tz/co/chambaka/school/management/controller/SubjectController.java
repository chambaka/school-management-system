package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.academic.SubjectRequest;
import tz.co.chambaka.school.management.dto.academic.SubjectResponse;
import tz.co.chambaka.school.management.service.SubjectService;
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
@RequestMapping("/api/v1/subjects")
@Tag(name = "Subjects")
public class SubjectController {

    private final SubjectService subjectService;
    private final TenantResolver tenantResolver;

    public SubjectController(SubjectService subjectService, TenantResolver tenantResolver) {
        this.subjectService = subjectService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public List<SubjectResponse> list() {
        return subjectService.list(tenantResolver.requireSchoolId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public SubjectResponse create(@Valid @RequestBody SubjectRequest request) {
        return subjectService.create(tenantResolver.requireSchoolId(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SubjectResponse update(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        return subjectService.update(tenantResolver.requireSchoolId(), id, request);
    }
}
