package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.common.PageResponse;
import tz.co.chambaka.school.management.dto.teacher.CreateTeacherRequest;
import tz.co.chambaka.school.management.dto.teacher.TeacherResponse;
import tz.co.chambaka.school.management.dto.teacher.UpdateTeacherRequest;
import tz.co.chambaka.school.management.security.CurrentUser;
import tz.co.chambaka.school.management.security.UserPrincipal;
import tz.co.chambaka.school.management.service.TeacherService;
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
@RequestMapping("/api/v1/teachers")
@Tag(name = "Teachers")
public class TeacherController {

    private final TeacherService teacherService;
    private final TenantResolver tenantResolver;

    public TeacherController(TeacherService teacherService, TenantResolver tenantResolver) {
        this.teacherService = teacherService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public PageResponse<TeacherResponse> list(Pageable pageable) {
        return teacherService.list(tenantResolver.requireSchoolId(), pageable);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('TEACHER')")
    public TeacherResponse me(@CurrentUser UserPrincipal principal) {
        return teacherService.get(tenantResolver.requireSchoolId(), teacherService.requireByUser(principal.getId()).getId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public TeacherResponse get(@PathVariable Long id) {
        return teacherService.get(tenantResolver.requireSchoolId(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public TeacherResponse create(@Valid @RequestBody CreateTeacherRequest request) {
        return teacherService.create(tenantResolver.requireSchoolId(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TeacherResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTeacherRequest request) {
        return teacherService.update(tenantResolver.requireSchoolId(), id, request);
    }
}
