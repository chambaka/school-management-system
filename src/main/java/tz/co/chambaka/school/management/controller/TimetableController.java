package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.academic.TimetableRequest;
import tz.co.chambaka.school.management.dto.academic.TimetableResponse;
import tz.co.chambaka.school.management.service.TimetableService;
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
@RequestMapping("/api/v1/timetable")
@Tag(name = "Timetable")
public class TimetableController {

    private final TimetableService timetableService;
    private final TenantResolver tenantResolver;

    public TimetableController(TimetableService timetableService, TenantResolver tenantResolver) {
        this.timetableService = timetableService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','TEACHER','STUDENT')")
    public List<TimetableResponse> bySection(@RequestParam Long sectionId) {
        return timetableService.bySection(tenantResolver.requireSchoolId(), sectionId);
    }

    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN','TEACHER')")
    public List<TimetableResponse> byTeacher(@PathVariable Long teacherId) {
        return timetableService.byTeacher(tenantResolver.requireSchoolId(), teacherId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    public TimetableResponse create(@Valid @RequestBody TimetableRequest request) {
        return timetableService.create(tenantResolver.requireSchoolId(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','TENANT_ADMIN')")
    public void delete(@PathVariable Long id) {
        timetableService.delete(tenantResolver.requireSchoolId(), id);
    }
}
