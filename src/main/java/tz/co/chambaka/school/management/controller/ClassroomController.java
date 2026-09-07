package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.academic.ClassroomRequest;
import tz.co.chambaka.school.management.dto.academic.ClassroomResponse;
import tz.co.chambaka.school.management.service.ClassroomService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/classrooms")
@Tag(name = "Classrooms")
public class ClassroomController {

    private final ClassroomService classroomService;
    private final TenantResolver tenantResolver;

    public ClassroomController(ClassroomService classroomService, TenantResolver tenantResolver) {
        this.classroomService = classroomService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','TEACHER')")
    public List<ClassroomResponse> list() {
        return classroomService.list(tenantResolver.requireSchoolId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ClassroomResponse create(@Valid @RequestBody ClassroomRequest request) {
        return classroomService.create(tenantResolver.requireSchoolId(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ClassroomResponse update(@PathVariable Long id, @Valid @RequestBody ClassroomRequest request) {
        return classroomService.update(tenantResolver.requireSchoolId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        classroomService.delete(tenantResolver.requireSchoolId(), id);
    }
}
