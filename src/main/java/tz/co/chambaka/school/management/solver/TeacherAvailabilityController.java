package tz.co.chambaka.school.management.solver;

import tz.co.chambaka.school.management.security.Access;
import tz.co.chambaka.school.management.tenant.TenantResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    public List<TeacherAvailabilityResponse> list(@RequestParam Long teacherId) {
        return availabilityService.list(tenantResolver.requireSchoolId(), teacherId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.TIMETABLE_MANAGE)
    public TeacherAvailabilityResponse create(@Valid @RequestBody TeacherAvailabilityRequest request) {
        return availabilityService.create(tenantResolver.requireSchoolId(), request);
    }
}
