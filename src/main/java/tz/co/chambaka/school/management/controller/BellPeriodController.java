package tz.co.chambaka.school.management.controller;

import tz.co.chambaka.school.management.dto.academic.BellPeriodRequest;
import tz.co.chambaka.school.management.dto.academic.BellPeriodResponse;
import tz.co.chambaka.school.management.security.Access;
import tz.co.chambaka.school.management.service.BellPeriodService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bell-periods")
@Tag(name = "Bell periods")
public class BellPeriodController {

    private final BellPeriodService bellPeriodService;
    private final TenantResolver tenantResolver;

    public BellPeriodController(BellPeriodService bellPeriodService, TenantResolver tenantResolver) {
        this.bellPeriodService = bellPeriodService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    @PreAuthorize(Access.TIMETABLE_VIEW)
    public List<BellPeriodResponse> list() {
        return bellPeriodService.list(tenantResolver.requireSchoolId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Access.TIMETABLE_MANAGE)
    public BellPeriodResponse create(@Valid @RequestBody BellPeriodRequest request) {
        return bellPeriodService.create(tenantResolver.requireSchoolId(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Access.TIMETABLE_MANAGE)
    public void delete(@PathVariable Long id) {
        bellPeriodService.delete(tenantResolver.requireSchoolId(), id);
    }
}
