package tz.co.chambaka.school.management.solver;

import tz.co.chambaka.school.management.security.Access;
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
@RequestMapping("/api/v1/teacher-availability")
@Tag(name = "Teacher availability")
public class TeacherAvailabilityController {

    private final TeacherAvailabilityService availabilityService;
    private final TenantResolver tenantResolver;

    public TeacherAvailabilityController(TeacherAvailabilityService availabilityService, TenantResolver tenantResolver) {
        this.availabilityService = availabilityService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize(Access.TIMETABLE_MANAGE)
    public List<TeacherAvailabilityResponse> list(@RequestParam(required = false) Long teacherId) {
        return availabilityService.list(tenantResolver.requireSchoolId(), teacherId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.TIMETABLE_MANAGE)
    public TeacherAvailabilityResponse create(@Valid @RequestBody TeacherAvailabilityRequest request) {
        return availabilityService.create(tenantResolver.requireSchoolId(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Access.TIMETABLE_MANAGE)
    public TeacherAvailabilityResponse update(@PathVariable Long id, @Valid @RequestBody TeacherAvailabilityRequest request) {
        return availabilityService.update(tenantResolver.requireSchoolId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.TIMETABLE_MANAGE)
    public void delete(@PathVariable Long id) {
        availabilityService.delete(tenantResolver.requireSchoolId(), id);
    }
}
